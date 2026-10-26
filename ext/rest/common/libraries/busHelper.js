"use strict";

const EventBus = require("vertx3-eventbus-client");
const R = require("ramda");
const path = require("path");
const helper = require(path.join(__dirname, "helper"));
const settings = require(path.join(__dirname, "..", "..", "..", "settings"));

class BusHelper {
  constructor(ebUrl) {
    this.eventBusUrl = R.defaultTo(settings.structureUrl("eventbus"), ebUrl);
    this.eb = new EventBus(this.eventBusUrl);
    this.intervalId = 0;
    const self = this;

    let closeRoutine = (error) => {
      console.log("Connection to Structure was closed. State: %s", self.eb.state);
      self.intervalId = setInterval(function () {
        if (self.eb.state === 1) {
          clearInterval(self.intervalId);
          self.eb.onclose = closeRoutine;
        } else {
          console.log("retrying connection... %s", self.eb.state);
          self.eb = new EventBus(self.eventBusUrl);
          self.eb.onopen = () => console.log("Connected to: %s", self.eventBusUrl);
        }
      }, 5000);
    };

    this.eb.onopen = () => console.log("Connected to: %s", self.eventBusUrl);
    this.eb.onclose = closeRoutine;
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
