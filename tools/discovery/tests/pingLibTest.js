"use strict";

const test = require('tape');
const R = require('ramda');
const pingLib = require('../pingLib');

test('ping a live host', function(t) {
    let host = 'localhost';
    pingLib.pingScan(host)
        .then((result) => {
            t.equal(result.host, 'localhost', 'Should be localhost entry');
            t.equal(result.alive, true, 'Should be alive');
            t.end();
        });
});

test('ping a dead host', function(t) {
    let host = '172.17.1.233';
    pingLib.pingScan(host)
        .then((result) => {
            t.equal(result.host, '172.17.1.233', 'Should be localhost entry');
            t.equal(result.alive, false, 'Should be dead');
            t.end();
        });
});