"use strict";

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');
const TypeDB = require('./TypeDB');

const busUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
const statusChannel = 'com.raaftech.structure.manager.discoverymanager';
const entityManagerChannel = 'com.raaftech.structure.manager.entitymanager';

class vClient {
    constructor() {
        this.eb = new EventBus(busUrl);
    }

    getAllTypes() {
        let self = this;
        return new Promise((resolve, reject) => {
            self.eb.onopen = function() {
                console.log('eventbus open');
                const base = 'com.raaftech.structure.repository.types';
                const msg = createOptions('getAll');
                self.eb.send(base, msg, (error, result) => {
                    if(error) {
                        reject('error in reading all types from structure');
                    } else {
                        let db = new TypeDB(result.body.result.data);
                        resolve(db);
                    }
                });
            }
        });
    }

    send(message) {
        this.eb.send(statusChannel, message);
    }

    createGraph(message) {
        let self = this;
        return new Promise((resolve, reject) => {
            self.eb.send(entityManagerChannel, message, (error, result) => {
                if(error) reject(error);
                else resolve(result);
            });
        });
    }

    close() {
        this.eb.close();
    }
}

function createOptions(cmd, args) {
  return R.zipObj(['msgh', 'args'], [R.defaultTo('help', cmd), R.defaultTo({}, args)]);
}

module.exports = vClient;
