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

//    Group.getAllGroups = function(callback) {
//    };
//    Group.createGroup = function(groupName, description, callback) {
//    };
//
//    Group.getChildren = function(groupUuid, callback) {
//    };
//
//    Group.addToGroup = function(groupUuid, instanceUuid, callback) {
//    };
};
