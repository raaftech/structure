"use strict";

const ping = require('ping');

const pingScan = (host) => {
    return new Promise((resolve, reject) => {
        ping.promise.probe(host)
            .then((res) => resolve({host: res.host, alive: res.alive}));
    });
};


module.exports = {
    pingScan: pingScan
};