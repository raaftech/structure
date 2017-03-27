'use strict';

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
const eb = new EventBus(eventBusUrl);
const notNil = R.compose(R.not, R.isNil);

module.exports = function(Admin) {

    Admin.structureInit = function(callback) {
        //todo step 1 request a purge structure db
        //todo step 2 request self init
        callback(null, {message: 'not implemented yet'});
    };

    Admin.remoteMethod('structureInit', {
        accepts: [],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/init'},
        description: 'Request structure bootstrap init'
    });


    Admin.structureStatus = function(callback) {
        callback(null, {message: 'not implemented yet'});
    };

    Admin.remoteMethod('structureStatus', {
        accepts: [],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/status'},
        description: 'Request current structure status info.'
    });


    //Disable a few endpoints that are not needed in Status channel
    let isStatic = true;
    Admin.disableRemoteMethod('find', isStatic);
    Admin.disableRemoteMethod('findOne', isStatic);
    Admin.disableRemoteMethod('findById', isStatic);
    Admin.disableRemoteMethod('exists', isStatic);
    Admin.disableRemoteMethod('upsert', isStatic);
    Admin.disableRemoteMethod('upsertWithWhere', isStatic);
    Admin.disableRemoteMethod('updateAll', isStatic);
    Admin.disableRemoteMethod('updateAttributes', isStatic);
    Admin.disableRemoteMethod('deleteById', isStatic);
    Admin.disableRemoteMethod('replaceById', isStatic);
    Admin.disableRemoteMethod('count', isStatic);
    Admin.disableRemoteMethod('create', isStatic);
    Admin.disableRemoteMethod('replaceOrCreate', isStatic);
};

function sendEvent(base, cmd, args) {
  return new Promise((resolve, reject) => {
    eb.send(base, createOptions(cmd, args), (error, result) => {
      if (error) reject(error);
      else resolve(result);
    });
  });
}
