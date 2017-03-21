"use strict";

const R = require('ramda');
const fetch = require('node-fetch');
const util = require('util');

const getBaseUrl = (host, port) => util.format('http://%s:%s/api',host, port);


const getEntityByName = (host, port, name) => {
    let url = util.format('%s/Entities/name/%s', getBaseUrl(host,port),name);
    return fetch(url).then(result => result.json());
};

const getEntityByUuid = (host, port, uuid) => {
    let url = util.format('%s/Entities/type/%s', getBaseUrl(host,port),uuid);
    return fetch(url).then(result => result.json());
};

const getChildren = (host, port, uuid) => {
    let url = util.format('%s/Entities/children/%s', getBaseUrl(host,port),uuid);
    return fetch(url).then(result => result.json());
};

const lastInstances = (host, port, quantity) => {
    let url = util.format('%s/Entities/last/%s', getBaseUrl(host,port),quantity);
    return fetch(url).then(result => result.json());
};

const getInstanceByUuid = (host, port, uuid) => {
    let url = util.format('%s/Entities/instance/%s', getBaseUrl(host,port),uuid);
    return fetch(url).then(result => result.json());
};


const createInstance = (host,port, token, options) => {
    let url = util.format('%s/Entities/instance?access_token=%s', getBaseUrl(host,port), token);
	return fetch(url,
                {
                    method: 'POST',
                    body: 'args='+JSON.stringify(options),
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                })
                .then(result => result.json());
};

const runInstrument = (host, port, token, instrument, uuid) => {

};

const login = (host, port, email, password) => {
    let url = util.format('%s/StructureUsers/login', getBaseUrl(host,port));
    let credentials = JSON.stringify({email: email, password: password});
    return fetch(url,
        {
            method: 'POST',
            body: credentials,
            headers: {'Content-Type': 'application/json'}
        })
        .then(result => result.json());
};

const logout = (host, port, token) => {
    let url = util.format('%s/StructureUsers/logout', getBaseUrl(host,port));
};

const createUser = (host, port, email, password) => {
    let url = util.format('%s/StructureUsers', getBaseUrl(host,port));
    let credentials = JSON.stringify({email: email, password: password});
    return fetch(url,
        {
            method: 'POST',
            body: credentials,
            headers: {'Content-Type': 'application/json'}
        })
        .then(result => result.json());
};

module.exports = {
    getEntityByName: getEntityByName,
    getEntityByUuid: getEntityByUuid,
    getChildren: getChildren,
    lastInstances: lastInstances,
    getInstanceByUuid: getInstanceByUuid,
	createInstance: createInstance,
    runInstrument: runInstrument,
    login: login,
    logout: logout,
    createUser: createUser
};
