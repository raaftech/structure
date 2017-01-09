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

const getEntityByName = (host, port, name) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/name/%s', baseUrl,name);
    return returnGetPromise(url);
};

const getEntityByUuid = (host, port, uuid) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/type/%s', baseUrl,uuid);
    return returnGetPromise(url);
};

const getChildren = (host, port, uuid) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/children/%s', baseUrl,uuid);
    return returnGetPromise(url);
};

const lastInstances = (host, port, quantity) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/last/%s', baseUrl,quantity);
    return returnGetPromise(url);
};

const getInstanceByUuid = (host, port, uuid) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/instance/%s', baseUrl,uuid);
    return returnGetPromise(url);
};

module.exports = {
    getEntityByName: getEntityByName,
    getEntityByUuid: getEntityByUuid,
    getChildren: getChildren,
    lastInstances: lastInstances,
    getInstanceByUuid: getInstanceByUuid
};