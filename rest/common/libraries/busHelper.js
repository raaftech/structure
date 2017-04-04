'use strict';

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, 'helper'));

class BusHelper {
    constructor(ebUrl) {
        this.eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', ebUrl);
        this.eb = new EventBus(this.eventBusUrl);

        this.eb.onopen = () => console.log('Connected to: %s', this.eventBusUrl);
        this.eb.onclose = (error) => console.log('Eventbus connection closed: %s', JSON.stringify(error, undefined,2));
    }

    sendEvent(base, cmd, args) {
        let self = this;
        return new Promise((resolve, reject) => {
            self.eb.send(base, helper.createOptions(cmd, args), (error, result) => {
                if (error) reject(error);
                else resolve(result);
            });
        });
    }
}

module.exports = BusHelper;