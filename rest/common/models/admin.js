'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));
const neoHelper = require(path.join(__dirname, '..','libraries','neoHelper'));

const evBus = require('../../server/server').eventBus;

const baseAddress = helper.getEventBusAddress('tech.raaf.structure.bus');
const managerAddress = baseAddress(['manager', 'structuremanager']);

const getResultData = helper.getResultData(['body','result','data']);
const getStatusData = helper.getResultData(['body','result']);

module.exports = function(Admin) {

    Admin.bootstrap = function(token, callback) {
        evBus.sendEvent(managerAddress, 'selfDiscovery', {token: token})
            .then(result => callback(null, getResultData(result)))
            .catch(ex => callback(ex));
    };

    Admin.remoteMethod('bootstrap', {
        accepts: [{arg: 'token', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/bootstrap/:token'},
        description: 'Request an action from structure'
    });

    Admin.structureInit = function(callback) {
        evBus.sendEvent(managerAddress, 'reinitialize', {})
            .then(result => callback(null, getResultData(result)))
            .catch((ex) => callback(ex));
    };

    Admin.remoteMethod('structureInit', {
        accepts: [],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/init'},
        description: 'Request structure bootstrap init'
    });

    Admin.reloadTypes = function(callback) {
        evBus.sendEvent(managerAddress, 'reload', {})
            .then(result => callback(null, getResultData(result)))
            .catch((ex) => callback(ex));
    };

    Admin.remoteMethod('reloadTypes', {
        accepts: [],
        returns: {root: true, type: 'object'},
        http: {verb: 'put', path: '/reload'},
        description: 'Reload structure types'
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
            .then(result => callback(null, R.path(['body','result'], result)))
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

    Admin.cypher = function(query, callback) {
        neoHelper.cypher(query)
            .then(result => callback(null, result))
            .catch(ex => callback(ex));
    };

    Admin.remoteMethod('cypher', {
        accepts: [{arg: 'query', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/cypher'},
        description: 'Arbitrary cypher query directly to neo4j backend'
    });
};
