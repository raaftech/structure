'use strict';

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','libraries','helper'));
const graphLib = require(path.join(__dirname, '..', 'libraries', 'graphLib'));

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
const createError = (status, msg) => {
    let error = new Error(msg);
    error.status = status;
    return error;
};

module.exports = function(Group) {
    Group.removeGroup = function(uuid, callback) {
        let message = {labels: ['Group'], uuid: uuid, includeStart: true};
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
        evBus.sendEvent(typesAddress, 'findOneByName', {name: 'ComponentGroup'})
            .then((result) => {
                return evBus.sendEvent(entityAddress, 'read',
                    {labels: ['Type'], start: getResultData(result).uuid, includeStart: true});
            })
            .then((entities) => {
                let entity = R.head(getResultData(entities));
                let instance = graphLib.createInstance(entity, {name: groupName, desc: description});
                instance.name = groupName;
                instance.desc = description;
                return [instance];
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

    Group.addToGroup = function(groupUuid, instanceUuid, callback) {
        let message = {grouping: groupUuid, groupee: instanceUuid};
        evBus.sendEvent(entityAddress, 'group', message)
            .then(result => callback(null, resultFetcher(result)))
            .catch(ex => callback(ex));
    };

    Group.remoteMethod('addToGroup', {
            accepts: [
                {arg: 'groupUuid', type: 'string', required: true},
                {arg: 'instanceUuid', type: 'string', required: true}
            ],
            returns: {root: true, type: 'object'},
            http: {verb: 'put', path: '/add/:instanceUuid/to/:groupUuid'},
            description: 'Add an instance to a grouping'
        });


    Group.removeFromGroup = function(groupUuid, instanceUuid, callback) {
        let message = {grouping: groupUuid, groupee: instanceUuid};
        evBus.sendEvent(entityAddress, 'ungroup', message)
            .then(result => callback(null, resultFetcher(result)))
            .catch(ex => callback(ex));
    };

    Group.remoteMethod('removeFromGroup', {
            accepts: [
                {arg: 'groupUuid', type: 'string', required: true},
                {arg: 'instanceUuid', type: 'string', required: true}
            ],
            returns: {root: true, type: 'object'},
            http: {verb: 'put', path: '/del/:instanceUuid/from/:groupUuid'},
            description: 'Remove an instance from a grouping'
        });
};
