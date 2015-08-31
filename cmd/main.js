#!/usr/bin/env node

var EventBus = require('vertx3-eventbus-client');

var eb = new EventBus('http://localhost:8080/eventbus');


eb.onopen = function () {
  eb.send('com.raaftech.structure.manager.busmanager', {"msgh":"list","args":{}}, function(data) {
    console.log(data);
    eb.close();
  });
}


