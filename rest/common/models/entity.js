'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));

const evBus = require('../../server/server').eventBus;

const baseAddress = helper.getEventBusAddress('tech.raaf.structure.bus');
const entityAddress = baseAddress(['manager', 'entitymanager']);
const typesAddress = baseAddress(['repository', 'types']);

const getResultData = helper.getResultData(['body','result','data']);
const getErrorData = helper.getResultData(['body','result', 'error']);
const resultFetcher = R.ifElse(
        R.compose(R.not, R.isNil, getResultData),
        getResultData,
        getErrorData
);

module.exports = function (Entity) {
    Entity.getAllTypes = function(callback) {
        evBus.sendEvent(typesAddress, 'getAll', {})
            .then(result => callback(null, resultFetcher(result)))
            .catch(ex => callback(ex));
    }

    Entity.remoteMethod('getAllTypes', {
        accepts: [],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/'},
        description: 'Fetch all types'
    });

    Entity.fetchByUuid = function (uuid, callback) {
        evBus.sendEvent(entityAddress, 'read',
                {labels: ['Type'], start: uuid, includeStart: true})
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    };

    Entity.remoteMethod('fetchByUuid', {
        accepts: [{arg: 'uuid', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/type/:uuid'},
        description: 'Fetch a type entity by uuid'
    });

    Entity.fetchByName = function (name, callback) {
        evBus.sendEvent(typesAddress, 'findOneByName', {name: name})
            .then((result) => {
                evBus.sendEvent(entityAddress, 'read',
                    {labels: ['Type'], start: getResultData(result).uuid, includeStart: true})
                    .then((res) => callback(null, R.head(getResultData(res))));
            })
            .catch((ex) => callback(ex));
    };

    Entity.remoteMethod('fetchByName', {
        accepts: [{arg: 'name', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/name/:name'},
        description: 'Fetch an entity by name'
    });

    Entity.getChildren = function (uuid, callback) {
        let children = function (id) {
            return evBus.sendEvent(baseAddress(['node', id]), 'getSubtypes', {})
                .then((result) => getResultData(result));
        };

        children(uuid)
            .then((kids) => {
                let promises = R.map((item) => {
                    return children(item.uuid)
                        .then((c) => {
                            item.leaf = (c.length === 0);
                            return item;
                        });
                }, kids);
                Promise.all(promises)
                    .then((ps) => {
                        let proms = R.map((item) => {
                            return evBus.sendEvent(entityAddress, 'read',
                                {labels: ['Type'], start: item.uuid, includeStart: true})
                                .then((result) => R.head(getResultData(result)));
                        }, ps);
                        Promise.all(proms)
                            .then(res => callback(null, res));
                    });
            })
            .catch((ex) => callback(ex));
    };

    Entity.remoteMethod('getChildren', {
        accepts: [{arg: 'uuid', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/children/:uuid'},
        description: 'Fetch subtypes for a given uuid'
    });

    Entity.getConstraints = function(uuid, callback) {
        const nodeAddress = baseAddress(['node', uuid]);

        evBus.sendEvent(nodeAddress, 'getConstraints',{})
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    }

    Entity.remoteMethod('getConstraints', {
            accepts: [{arg: 'uuid', type: 'string', required: true}],
            returns: {root: true, type: 'object'},
            http: {verb: 'get', path: '/constraints/:uuid'},
            description: 'Fetch constraints for a type by uuid'
        });


//INSTRUMENTS
    Entity.getInstruments = function (uuid, callback) {
        evBus.sendEvent(baseAddress(['node', uuid]), 'getInstruments', {})
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    };

    Entity.remoteMethod('getInstruments', {
        accepts: [{arg: 'uuid', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/instrument/:uuid'},
        description: 'Fetch instruments list by uuid'
    });

    Entity.runInstrument = function (instrumentName, uuid, callback) {
        let args = {
            instrument: instrumentName
        };
        //FIXME find a beter way to do this
        if (instrumentName === 'IpAddressDiscover') args.timeAllocated = 600000;

        evBus.sendEvent(baseAddress(['node', uuid]), 'runInstrument', args)
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    };

    Entity.remoteMethod('runInstrument', {
        accepts: [{arg: 'instrumentName', type: 'string', required: true},
            {arg: 'uuid', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/instrument'},
        description: 'run an instrument by uuid'
    });
};
