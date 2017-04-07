'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));
const BusHelper = require(path.join(__dirname, '..','libraries','busHelper'));

const evBus = new BusHelper(process.env.EVENTBUS);

const baseAddress = helper.getEventBusAddress('com.raaftech.structure');
const entityAddress = baseAddress(['manager', 'entitymanager']);
const typesAddress = baseAddress(['repository', 'types']);

const getResultData = helper.getResultData(['body','result','data']);

// eb.onopen = () => console.log('Connected to: %s', eventBusUrl);
// eb.onclose = (error) => console.log('Eventbus connection closed: %s', stringify(error));

module.exports = function (Entity) {
    Entity.fetchByUuid = function (uuid, callback) {
        evBus.sendEvent(entityAddress, 'read',
                {labels: ['type'], start: uuid, includeStart: true})
            .then((result) => callback(null, getResultData(result)))
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
                    {labels: ['type'], start: getResultData(result).uuid, includeStart: true})
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
                                {labels: ['type'], start: item.uuid, includeStart: true})
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


//INSTRUMENTS
    Entity.getInstruments = function (uuid, callback) {
        evBus.sendEvent(baseAddress(['node', 'component', uuid]), 'getInstruments', {})
            .then((result) => callback(null, getResultData(result)))
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

        evBus.sendEvent(baseAddress(['node', 'component', uuid]), 'runInstrument', args)
            .then((result) => callback(null, getResultData(result)))
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
