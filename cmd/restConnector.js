"use strict";

const R = require('ramda');
const agent = require('superagent');
const util = require('util');

const getEntityByName = (host, port, name) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/name/%s', baseUrl,name);
    return new Promise((resolve, reject) => {
        agent.get(url)
            .end(function(error, result) {
                if(error) reject(error);
                else resolve(result.body.result);
            });
    });
};

const getEntityByUuid = (host, port, uuid) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/type/%s', baseUrl,uuid);
    return new Promise((resolve, reject) => {
        agent.get(url)
            .end(function(error, result) {
                if(error) reject(error);
                else resolve(result.body.result);
            });
    });
};

const getChildren = (host, port, uuid) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/children/%s', baseUrl,uuid);
    return new Promise((resolve, reject) => {
        agent.get(url)
            .end(function(error, result) {
                if(error) reject(error);
                else resolve(result.body.result);
            });
    });
};

const lastInstances = (host, port, quantity) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/last/%s', baseUrl,quantity);
    return new Promise((resolve, reject) => {
        agent.get(url)
            .end(function(error, result) {
                if(error) reject(error);
                else resolve(result.body.result);
            });
    });
};

const getInstanceByUuid = (host, port, uuid) => {
    let baseUrl = util.format('http://%s:%s/api',host, port);
    let url = util.format('%s/Entities/instance/%s', baseUrl,uuid);
    return new Promise((resolve, reject) => {
        agent.get(url)
            .end(function(error, result) {
                if(error) reject(error);
                else resolve(result.body.result);
            });
    });
};

module.exports = {
    getEntityByName: getEntityByName,
    getEntityByUuid: getEntityByUuid,
    getChildren: getChildren,
    lastInstances: lastInstances,
    getInstanceByUuid: getInstanceByUuid
};