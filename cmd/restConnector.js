"use strict";

const R = require('ramda');
const fetch = require('node-fetch');
const util = require('util');

const getBaseUrl = (host, port) => util.format('http://%s:%s/api',host, port);

class RestConnector {
    constructor(host, port) {
        this.host = host;
        this.port = port;
        this.token;
    }

    getToken() {
        return this.token;
    }

    setToken(token) {
        this.token = token;
    }

    getChangeStreamUrl() {
        return util.format('http://%s:%s/api/Statuses/change-stream',this.host,this.port);
    }

    getEntityByName(name) {
        let url = util.format('%s/Entities/name/%s', getBaseUrl(this.host,this.port),name);
        return fetch(url).then(result => result.json());
    }

    getEntityByUuid(uuid) {
        let url = util.format('%s/Entities/type/%s', getBaseUrl(this.host,this.port),uuid);
        return fetch(url)
            .then(result => result.json())
            .then(list => R.head(list));
    }

    getChildren(uuid) {
        let url = util.format('%s/Entities/children/%s', getBaseUrl(this.host,this.port),uuid);
        return fetch(url).then(result => result.json());
    }

    getConstraints(uuid) {
        let url = util.format('%s/Entities/constraints/%s', getBaseUrl(this.host,this.port),uuid);
        return fetch(url).then(result => result.json());
    }

    lastInstances(quantity) {
        let url = util.format('%s/Instances/last/%s', getBaseUrl(this.host,this.port),quantity);
        return fetch(url).then(result => result.json());
    }

    getInstanceByUuid(uuid) {
        let url = util.format('%s/Instances/uuid/%s', getBaseUrl(this.host,this.port),uuid);
        return fetch(url).then(result => result.json());
    }

    getInstanceByName(name) {
        let url = util.format('%s/Instances/name/%s', getBaseUrl(this.host,this.port),name);
        return fetch(url).then(result => result.json());
    }

    getInstanceByPredicate(property, value) {
        let url = util.format('%s/Instances/predicate', getBaseUrl(this.host,this.port));
        return fetch(url,
            {
                method: 'POST',
                body: util.format('property=%s&value=%s', property, value),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    createInstance(options) {
        let url = util.format('%s/Instances?access_token=%s', getBaseUrl(this.host,this.port), this.token);
        return fetch(url,
            {
                method: 'POST',
                body: 'args='+JSON.stringify(options),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    updateInstance(options) {
        let url = util.format('%s/Instances?access_token=%s', getBaseUrl(this.host,this.port), this.token);
        return fetch(url,
            {
                method: 'PUT',
                body: 'args='+JSON.stringify(options),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    removeInstance(uuid) {
        let url = util.format('%s/Instances/%s?access_token=%s', getBaseUrl(this.host,this.port), uuid, this.token);
        return fetch(url,{method: 'DELETE'})
                    .then(result => result.json());
    }

    specializeInstance(instanceUuid, isaUuid) {
        let url = util.format('%s/Instances/specialize?access_token=%s', getBaseUrl(this.host,this.port), this.token);
        return fetch(url,
                    {
                        method: 'PUT',
                        body: 'instanceUuid='+instanceUuid+'&isaUuid='+isaUuid,
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    })
                    .then(result => result.json());
    }

    createGroup(groupName, description) {
        let url = util.format('%s/Groups?access_token=%s', getBaseUrl(this.host,this.port), this.token);
        return fetch(url,
            {
                method: 'POST',
                body: util.format('groupName=%s&description=%s',groupName,description),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    removeGroup(uuid) {
        let url = util.format('%s/Groups/%s?access_token=%s', getBaseUrl(this.host,this.port), uuid, this.token);
        return fetch(url,{method: 'DELETE'})
                    .then(result => result.json());
    }

    addToGroup(groupUuid, instanceUuid) {
        let url = util.format('%s/Groups/add/%s/to/%s?access_token=%s', getBaseUrl(this.host,this.port), instanceUuid, groupUuid, this.token);
        return fetch(url,
            {
                method: 'PUT',
                body: '',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    getInstruments(uuid) {
        let url = util.format('%s/Instances/instruments/%s', getBaseUrl(this.host,this.port),uuid);
        return fetch(url).then(result => result.json());
    }

    runInstrument(uuid, instrument, options) {
        let url = util.format('%s/Instances/run/%s/on/%s?access_token=%s', getBaseUrl(this.host,this.port), instrument, uuid, this.token);
        let args = {
            instrument: instrument,
            arguments: options
        };

        return fetch(url,
            {
                method: 'POST',
                body: 'options='+JSON.stringify(args),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    runCapability(uuid, capability) {
        let url = util.format('%s/Instances/exec/%s/on/%s?access_token=%s', getBaseUrl(this.host,this.port), capability, uuid, this.token);
        let args = {
            capability: capability
        };
        console.log(args);
        return fetch(url,
            {
                method: 'POST',
                body: 'options='+JSON.stringify(args),
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    bootstrap() {
        const self = this;
        let url = util.format('%s/Admins/bootstrap/%s?access_token=%s', getBaseUrl(this.host, this.port), this.token, this.token);
        return fetch(url).then(result => result.json());
    }

    login(email,password) {
        const self = this;
        let url = util.format('%s/StructureUsers/login', getBaseUrl(this.host,this.port));
        let credentials = JSON.stringify({email: email, password: password});
        return fetch(url,
            {
                method: 'POST',
                body: credentials,
                headers: {'Content-Type': 'application/json'}
            })
            .then(result => result.json())
            .then(creds => {
                self.token = creds.id;
                return creds;
            });
    }

    logout() {
        let url = util.format('%s/StructureUsers/logout', getBaseUrl(this.host,this.port));
    }

    createUser(email, password) {
        let url = util.format('%s/StructureUsers', getBaseUrl(this.host,this.port));
        let credentials = JSON.stringify({email: email, password: password});
        return fetch(url,
            {
                method: 'POST',
                body: credentials,
                headers: {'Content-Type': 'application/json'}
            })
            .then(result => result.json());
    }

    getStructureStatusInfo() {
        let url = util.format('%s/Admins/status', getBaseUrl(this.host, this.port));
        return fetch(url).then(result => result.json());
    }

    purgeInit() {
        let url = util.format('%s/Admins/init?access_token=%s', getBaseUrl(this.host,this.port), this.token);
        return fetch(url,
            {
                method: 'POST',
                body: '',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    reloadTypes() {
        let url = util.format('%s/Admins/reload?access_token=%s', getBaseUrl(this.host,this.port), this.token);
        return fetch(url,
            {
                method: 'PUT',
                body: '',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            .then(result => result.json());
    }

    query(address, command, payload) {
        let url = util.format('%s/admins/query?access_token=%s',getBaseUrl(this.host,this.port), this.token);
        let data = util.format('address=%s&command=%s&payload=%s',encodeURIComponent(address),
                                encodeURIComponent(command), encodeURIComponent(JSON.stringify(payload)));
        return fetch(url, {
            method: 'POST',
            body: data,
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).then(result => result.json());
    }
}

module.exports = RestConnector;
