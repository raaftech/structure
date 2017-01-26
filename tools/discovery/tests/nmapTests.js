"use strict";

const test = require('tape');
const R = require('ramda');
const nmapLib = require('../nmapLib');

//Function to check if a given object has all specified properties.
const itemPropertyTest = R.allPass([
    R.has('hostname'),
    R.has('ip'),
    R.has('openPorts')
]);
//Function to check if the length of an array is greater or equal to 35
const lengthCheck = R.pipe(R.length, R.lt(35));
const stringify = (str) => JSON.stringify(str, undefined, 2);
const getOpenPorts = R.pipe(R.project(['ip', 'openPorts']), stringify);

test('test quick scan', function(tt) {

    tt.test('quick scan raaf network', function(t) {
        let cidr = '172.17.1.0/24';
        nmapLib.quickScan(cidr)
            .then((result) => {
				t.ok(lengthCheck(result), 'should be at least 35 entries in the result');
                let res = R.reduce(R.and, true, R.map(itemPropertyTest, result));
                t.ok(res, 'all items should have all properties');
                t.end();
            })
            .catch((ex) => {
                t.end(ex);
            });
    });
});

test('test nmap scan', function(tt) {

    tt.test('nmap scan raaf network', function(t) {
        let cidr = '172.17.1.0/24';
        let args = '-p22,80';
        nmapLib.nmapScan(cidr,args)
            .then((result) => {
				t.ok(lengthCheck(result), 'should be at least 35 entries in the result');
                let res = R.reduce(R.and, true, R.map(itemPropertyTest, result));
                t.ok(res, 'all items should have all properties');
                t.end();
            })
            .catch((ex) => {
                t.end(ex);
            });
    });
});

