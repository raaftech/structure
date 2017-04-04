'use strict';

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
const eb = new EventBus(eventBusUrl);

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
        sendEvent(getEventBusAddress(['manager', 'structuremanager']), 'status', {})
                    .then((result) => callback(null, R.path(['body', 'result'])(result)))
                    .catch((ex) => callback(ex));
    };

    Admin.remoteMethod('structureStatus', {
        accepts: [],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/status'},
        description: 'Request current structure status info.'
    });

    Admin.query = function (address, command, payload, callback) {
        sendEvent(address, command, payload)
            .then(result => callback(null, getResultData(result)))
            .catch(ex => callback(ex));

    };

    Admin.remoteMethod('query', {
        accepts: [{arg: 'address', type: 'string', required: true},
            {arg: 'command', type: 'string', required: true},
            {arg: 'payload', type: 'object', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/query'},
        description: 'Request an action from structure'
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

function createOptions(cmd, args) {
  return R.zipObj(['msgh', 'args'], [R.defaultTo('help', cmd), R.defaultTo({}, args)]);
}

function getEventBusAddress(parts) {
  return R.join('.', R.prepend('com.raaftech.structure', parts));
}

function getResultData(result) {
  return R.path(['body', 'result', 'data'])(result);
}
