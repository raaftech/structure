'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));
const BusHelper = require(path.join(__dirname, '..','libraries','busHelper'));

const evBus = new BusHelper(process.env.EVENTBUS);

const baseAddress = helper.getEventBusAddress('com.raaftech.structure');
const instanceAddress = baseAddress(['repository', 'instances']);
const entityAddress = baseAddress(['manager', 'entitymanager']);

const getResultData = helper.getResultData(['body','result','data']);

module.exports = function (Instance) {
    Instance.lastCreated = function (quantity, callback) {
        evBus.sendEvent(instanceAddress, 'nLastCreated', {amount: quantity})
            .then((result) => {
                let promises = R.map((item) => {
                    return evBus.sendEvent(entityAddress,
                        'read',
                        {labels: ['type'], start: item.uuid, includeStart: true})
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
        evBus.sendEvent(entityAddress,
            'read',
            {labels: ['instance'], start: uuid, includeStart: true})
            .then((res) => callback(null, R.head(getResultData(res))));
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
                        {labels: ['instance'], start: uuid, includeStart: true}));
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
            .then((result) => callback(null, getResultData(result)))
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
            .then((result) => callback(null, getResultData(result)))
            .catch((ex) => callback(ex));
    };

    Instance.remoteMethod('updateInstance', {
        accepts: [{arg: 'args', type: 'object', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'put', path: '/'},
        description: 'update an existing instance'
    });
};
