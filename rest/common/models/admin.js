'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));
const BusHelper = require(path.join(__dirname, '..','libraries','busHelper'));

const evBus = new BusHelper(process.env.EVENTBUS);

const baseAddress = helper.getEventBusAddress('com.raaftech.structure');
const managerAddress = baseAddress(['manager', 'structuremanager']);

const getResultData = helper.getResultData(['body','result','data']);
const getStatusData = helper.getResultData(['body','result']);

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
        evBus.sendEvent(managerAddress, 'status', {})
                    .then((result) => callback(null, getStatusData(result)))
                    .catch((ex) => callback(ex));
    };

    Admin.remoteMethod('structureStatus', {
        accepts: [],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/status'},
        description: 'Request current structure status info.'
    });

    Admin.query = function (address, command, payload, callback) {
        evBus.sendEvent(address, command, payload)
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
};
