'use strict';

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
let eb = new EventBus(eventBusUrl);
let intervalId = 0;
const notNil = R.compose(R.not, R.isNil);

module.exports = function (Status) {
    function wrapper() {
        return new Promise((resolve, reject) => {
            eb.onopen = () => resolve('Connected to status channel');
            let closeRoutine = (error) => {
                console.log('Connection to Structure was closed. State: %s', eb.state);
                intervalId = setInterval(function() {
                    if(eb.state === 1) {
                        clearInterval(intervalId);
                        eb.onclose = closeRoutine;
                        setupStatusChannel();
                    } else {
                        console.log('retrying status connection... %s', eb.state);
                        eb = new EventBus(eventBusUrl);
                        eb.onopen = () => console.log('Connected to status channel');
                    }
                }, 5000);
            };

            eb.onclose = closeRoutine;
        });
    }

    //TODO think about if we want to persist status messages
    //FIXME if not, we need a way to auto clean status message resources in memory db
    function setupStatusChannel() {
        eb.registerHandler('com.raaftech.structure.status', function(error, message) {
			console.log(message);
            if(error) {
                console.log("ERROR: "+error);
            } else {
                let msg = R.prop('body',message);
                if(notNil(msg)) {
                    Status.create(msg, function (e, r) {
                        if (e) {
                            console.log('error while creating resource: ' + e);
                        } else {
                            //console.log('resource created: ' + stringify(r));
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
