"use strict";

const R = require('ramda');
const agent = require('superagent');
const util = require('util');

//Construct a default GET based return function for use by superagent
const getReturnFunction = (resolve, reject) => {
    return function(error, result) {
        if(error) reject(error);
        else resolve(R.path(['body', 'result'], result));
    };
};

//Construct a new Promise based on a url
const returnGetPromise = (url) => {
    return new Promise((resolve, reject) => {
        agent.get(url)
            .end(getReturnFunction(resolve, reject));
    });
};

const getBaseUrl = (host, port) => util.format('http://%s:%s/api',host, port);

const getEntityByName = (host, port, name) => {
    let url = util.format('%s/Entities/name/%s', getBaseUrl(host,port),name);
    return returnGetPromise(url);
};

const getEntityByUuid = (host, port, uuid) => {
    let url = util.format('%s/Entities/type/%s', getBaseUrl(host,port),uuid);
    return returnGetPromise(url);
};

const getChildren = (host, port, uuid) => {
    let url = util.format('%s/Entities/children/%s', getBaseUrl(host,port),uuid);
    return returnGetPromise(url);
};

const lastInstances = (host, port, quantity) => {
    let url = util.format('%s/Entities/last/%s', getBaseUrl(host,port),quantity);
    return returnGetPromise(url);
};

const getInstanceByUuid = (host, port, uuid) => {
    let url = util.format('%s/Entities/instance/%s', getBaseUrl(host,port),uuid);
    return returnGetPromise(url);
};

module.exports = {
    getEntityByName: getEntityByName,
    getEntityByUuid: getEntityByUuid,
    getChildren: getChildren,
    lastInstances: lastInstances,
    getInstanceByUuid: getInstanceByUuid
};