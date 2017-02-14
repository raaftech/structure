"use strict";

const test = require('tape');
const R = require('ramda');
const nmapLib = require('../nmapLib');
const itemPropertyTest = R.allPass([
    R.has('hostname'),
    R.has('ip'),
    R.has('openPorts')
]);

test('test quick scan', function(tt) {

    tt.test('quick scan raaf network', function(t) {
        let cidr = '172.17.1.0/24';
        nmapLib.quickScan(cidr)
            .then((result) => {
				t.ok(R.length(result) >= 35, 'should be at least 35 entries');
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
        let args = '-p22';
        nmapLib.nmapScan(cidr,args)
            .then((result) => {
				t.ok(R.length(result) >= 35, 'should be at least 35 entries');
                let res = R.reduce(R.and, true, R.map(itemPropertyTest, result));
                t.ok(res, 'all items should have all properties');
                t.end();
            })
            .catch((ex) => {
                t.end(ex);
            });
    });
});

