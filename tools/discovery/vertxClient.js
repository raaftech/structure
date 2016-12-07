"use strict";

const util = require('util');
const EventBus = require('vertx3-eventbus-client');

const busUrl = 'http://localhost:8080/eventbus';
const statusChannel = 'com.raaftech.structure.manager.discoverymanager';

function vClient() {
    this.eb = new EventBus(busUrl);
    this.eb.onopen = function() {
        console.log('eventbus open');
    };
}

vClient.prototype.send = function(message) {
    this.eb.send(statusChannel, message);
};

vClient.prototype.close = function() {
    this.eb.close();
};

module.exports = vClient;
