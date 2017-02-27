'use strict';

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
const eb = new EventBus(eventBusUrl);
const notNil = R.compose(R.not, R.isNil);

module.exports = function (Status) {
    function wrapper() {
        return new Promise((resolve, reject) => {
            eb.onopen = () => resolve('Connected to status channel');
            eb.onclose = (error) => resolve('Error while connecting to status channel: '+error.reason);
        });
    }

    //TODO think about if we want to persist status messages
    //FIXME if not, we need a way to auto clean status message resources in memory db
    function setupStatusChannel() {
        eb.registerHandler('com.raaftech.structure.status', function(error, message) {
            if(error) {
                console.log("ERROR: "+error);
            } else {
                let msg = R.prop('body',message);
                if(notNil(msg)) {
                    Status.create(msg, function (e, r) {
                        if (e) {
                            console.log('error while creating resource: ' + e);
                        } else {
                            console.log('resource created: ' + stringify(r));
                        }
                    });
                } else {
                    console.log('Error while parsing message: '+stringify(message));
                }
            }
        });
    }

    wrapper().then((res) => {
        console.log(res);
        setupStatusChannel();
    });


    //Disable a few endpoints that are not needed in Status channel
    let isStatic = true;
    Status.disableRemoteMethod('find', isStatic);
    Status.disableRemoteMethod('findOne', isStatic);
    Status.disableRemoteMethod('findById', isStatic);
    Status.disableRemoteMethod('exists', isStatic);
    Status.disableRemoteMethod('upsert', isStatic);
    Status.disableRemoteMethod('upsertWithWhere', isStatic);
    Status.disableRemoteMethod('updateAll', isStatic);
    Status.disableRemoteMethod('updateAttributes', false);
    Status.disableRemoteMethod('deleteById', isStatic);
    Status.disableRemoteMethod('replaceById', isStatic);
    Status.disableRemoteMethod('count', isStatic);
    Status.disableRemoteMethod('create', isStatic);
    Status.disableRemoteMethod('replaceOrCreate', isStatic);
};

function stringify(json) {
    return JSON.stringify(json, undefined, 2);
}
