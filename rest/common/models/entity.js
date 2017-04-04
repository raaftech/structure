'use strict';

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
const eb = new EventBus(eventBusUrl);

eb.onopen = () => console.log('Connected to: %s', eventBusUrl);
eb.onclose = (error) => console.log('Eventbus connection closed: %s', stringify(error));

module.exports = function (Entity) {

//DEVELOPMENT endpoint

    Entity.query = function (address, command, payload, callback) {
        sendEvent(address, command, payload)
            .then(result => callback(null, getResultData(result)))
            .catch(ex => callback(ex));

    };

    Entity.remoteMethod('query', {
        accepts: [{arg: 'address', type: 'string', required: true},
            {arg: 'command', type: 'string', required: true},
            {arg: 'payload', type: 'object', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/query'},
        description: 'Request an action from structure'
    });

//TYPES
    Entity.fetchByUuid = function (uuid, callback) {
        sendEvent(getEventBusAddress(['manager', 'entitymanager']),
            'read',
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
        sendEvent(getEventBusAddress(['repository', 'types']), 'findOneByName', {name: name})
            .then((result) => {
                sendEvent(getEventBusAddress(['manager', 'entitymanager']),
                    'read',
                    {labels: ['type'], start: result.body.result.data.uuid, includeStart: true})
                    .then((result) => callback(null, R.head(getResultData(result))));
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
            return sendEvent(getEventBusAddress(['node', 'type', id]), 'getSubtypes', {})
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
                            return sendEvent(getEventBusAddress(['manager', 'entitymanager']),
                                'read',
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
        sendEvent(getEventBusAddress(['node', 'component', uuid]), 'getInstruments', {})
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

        sendEvent(getEventBusAddress(['node', 'component', uuid]), 'runInstrument', args)
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

function stringify(json) {
    return JSON.stringify(json, undefined, 2);
}
