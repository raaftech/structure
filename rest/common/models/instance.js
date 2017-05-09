'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));
const evBus = require('../../server/server').eventBus;

const baseAddress = helper.getEventBusAddress('com.raaftech.structure');
const instanceAddress = baseAddress(['repository', 'instances']);
const entityAddress = baseAddress(['manager', 'entitymanager']);

const getResultData = helper.getResultData(['body','result','data']);
const getErrorData = helper.getResultData(['body','result', 'error']);
const resultFetcher = R.ifElse(
        R.compose(R.not, R.isNil, getResultData),
        getResultData,
        getErrorData
);

const createError = (status, msg) => {
    let error = new Error(msg);
    error.status = status;
    return error;
};

module.exports = function (Instance) {
    Instance.lastCreated = function (quantity, callback) {
        evBus.sendEvent(instanceAddress, 'nLastCreated', {amount: quantity})
            .then((result) => {
                let promises = R.map((item) => {
                    return evBus.sendEvent(entityAddress,
                        'read',
                        {labels: ['Type'], start: item.uuid, includeStart: true})
                        .then((res) => R.head(getResultData(res)));
                }, getResultData(result));
                Promise.all(promises)
                    .then(res => callback(null, res));
            })
            .catch((ex) => callback(ex));
    };

    Instance.remoteMethod('lastCreated', {
        accepts: [{arg: 'quantity', type: 'integer', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/last/:quantity'},
        description: 'Fetch the last created instances'
    });

    Instance.fetchInstance = function (uuid, callback) {
        let message = {labels: ['Instance'], start: uuid, includeStart: true};
        evBus.sendEvent(entityAddress, 'read', message)
            .then((res) => {
                let result = R.path(['body','result'], res);
                let getFirstResult = R.compose(R.head, R.prop('data'));
                if(R.has('data', result)) {
                    callback(null, getFirstResult(result));
                } else {
                    callback(createError(404, result.error));
                }
            })
            .catch(ex => callback(ex));
    };

    Instance.remoteMethod('fetchInstance', {
        accepts: [{arg: 'uuid', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/uuid/:uuid'},
        description: 'Fetch an instance by uuid'
    });

    Instance.fetchInstanceByName = function (name, callback) {
        evBus.sendEvent(instanceAddress, 'findByName', {name: name})
            .then((result) => {
                let uuidList = getResultData(result);
                uuidList = R.map(R.prop('uuid'), uuidList);

                let promises = [];
                R.forEach(uuid => {
                    promises.push(evBus.sendEvent(entityAddress,
                        'read',
                        {labels: ['Instance'], start: uuid, includeStart: true}));
                }, uuidList);
                Promise.all(promises)
                    .then(res => {
                        let result = R.map(R.pipe(getResultData, R.head), res);
                        callback(null, result);
                    });
            })
            .catch((ex) => callback(ex));
    };

    Instance.remoteMethod('fetchInstanceByName', {
        accepts: [{arg: 'name', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/name/:name'},
        description: 'Fetch an instance by name'
    });

    Instance.createInstance = function (args, callback) {
        evBus.sendEvent(entityAddress, 'create', args)
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    };

    Instance.remoteMethod('createInstance', {
        accepts: [{arg: 'args', type: 'object', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/'},
        description: 'create a new instance'
    });

    Instance.updateInstance = function (args, callback) {
        evBus.sendEvent(entityAddress, 'update', args)
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    };

    Instance.remoteMethod('updateInstance', {
        accepts: [{arg: 'args', type: 'object', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'put', path: '/'},
        description: 'update an existing instance'
    });

    Instance.specialize = function(instanceUuid, isaUuid, callback) {
        evBus.sendEvent(entityAddress, 'specialize', {instance: instanceUuid, isa: isaUuid})
            .then(result => callback(null, resultFetcher(result)))
            .catch(ex => callback(ex));
    };

    Instance.remoteMethod('specialize', {
        accepts: [
            {arg: 'instanceUuid', type: 'string', required: true},
            {arg: 'isaUuid', type: 'string', required: true}
        ],
        returns: {root: true, type: 'object'},
        http: {verb: 'put', path: '/specialize'},
        description: 'specialize an existing instance'
    });


    Instance.removeInstance = function (uuid, callback) {
        let message = {labels: ['Instance'], uuid: uuid, includeStart: true};
        evBus.sendEvent(entityAddress, 'delete', message)
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    };

    Instance.remoteMethod('removeInstance', {
        accepts: [{arg: 'uuid', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'delete', path: '/:uuid'},
        description: 'remove an existing instance'
    });
};
