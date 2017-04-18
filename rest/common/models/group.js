'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));
const BusHelper = require(path.join(__dirname, '..','libraries','busHelper'));
const Structure = require(path.join(__dirname, '..','libraries','subgraph'));

const evBus = new BusHelper(process.env.EVENTBUS);

const baseAddress = helper.getEventBusAddress('com.raaftech.structure');
const instanceAddress = baseAddress(['repository', 'instances']);
const entityAddress = baseAddress(['manager', 'entitymanager']);
const typesAddress = baseAddress(['repository', 'types']);

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

module.exports = function(Group) {
    Group.removeGroup = function(uuid, callback) {
        let message = {labels: ['Grouping'], uuid: uuid, includeStart: true};
        evBus.sendEvent(entityAddress, 'delete', message)
            .then((result) => callback(null, getResultData(result)))
            .catch((ex) => callback(ex));
    };

    Group.remoteMethod('removeGroup', {
        accepts: [{arg: 'uuid', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'delete', path: '/:uuid'},
        description: 'remove an existing grouping'
    });

    Group.createGroup = function(groupName, description, callback) {
        evBus.sendEvent(typesAddress, 'findOneByName', {name: 'Grouping'})
            .then((result) => {
                return evBus.sendEvent(entityAddress, 'read',
                    {labels: ['Type'], start: getResultData(result).uuid, includeStart: true});
            })
            .then((entities) => {
                let entity = R.head(getResultData(entities));
                let baseType = helper.json2Entity(false, entity);
                entity = R.pipe(
                    R.assoc('type', 'Grouping'),
                    R.assoc('name', groupName),
                    R.assoc('desc', description),
                    R.dissocPath(['meta','checksum']),
                    R.dissocPath(['meta','relations'])) (entity);

                let instance = helper.createInstance(baseType, helper.json2Entity(true, entity));
                return new Structure.SubGraph(instance).toList();
            })
            .then(entityList => evBus.sendEvent(entityAddress, 'create', {entities: entityList}))
            .then((result) => callback(null, resultFetcher(result)))
            .catch((ex) => callback(ex));
    };

    Group.remoteMethod('createGroup', {
        accepts: [
            {arg: 'groupName', type: 'string', required: true},
            {arg: 'description', type: 'string', required: false}
        ],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/'},
        description: 'Create a new group'
    });

//    Group.getAllGroups = function(callback) {
//    };
//
//    Group.getChildren = function(groupUuid, callback) {
//    };
//
//    Group.addToGroup = function(groupUuid, instanceUuid, callback) {
//    };
};
