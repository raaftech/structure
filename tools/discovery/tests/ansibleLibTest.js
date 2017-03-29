"use strict";

const test = require('tape');
const lib = require('../ansibleLib');
const R = require('ramda');

const ipCheck = (ip) => R.pipe(R.path(['default_ipv4','address']), R.equals(ip));
const distCheck = (dist) => R.propEq('distribution', dist);
const hostnameCheck = (name) => R.propEq('hostname', name);

test.only('setup tests', function (tt) {
    tt.test('localhost setup scan test', function (t) {
        lib.setup('localhost')
            .then((result) => {
                console.log(result);
                t.ok(R.is(Object,result), 'result should be an object');
                t.ok(R.has('default_ipv4', result), 'should have a default_ipv4 property');
                t.ok(R.has('distribution', result), 'should have a distribution property');
                t.ok(R.has('hostname', result), 'should have a hostname property');
                t.end();
            })
            .catch((ex) => {
                console.log(ex);
                t.end(JSON.stringify(ex));
            });
    });

    tt.test('orient.raaf.local setup scan test', function (t) {
        lib.setup('172.17.1.62')
            .then((result) => {
                console.log(result);
                 let ipc = ipCheck('172.17.1.62')(result);
                 let distc = distCheck('OpenBSD')(result);
                 let namec = hostnameCheck('orient')(result);

                 t.ok(R.is(Object,result), 'result should be an object');
                 t.ok(ipc, 'Should be ip of orient (172.17.1.62)');
                 t.ok(distc, 'Should be OpenBSD distro');
                 t.ok(namec, 'Should have hostname "orient"');

                t.end();
            })
            .catch((ex) => {
                t.end(JSON.stringify(ex));
            });
    });
});

test('shell tests', function (tt) {
    tt.test('localhost shell ls -al test', function (t) {
        lib.shell('localhost', 'ls -al')
            .then((result) => {
                console.log(result);
                t.end();
            })
            .catch((ex) => {
                t.end(JSON.stringify(ex));
            });
    });
});
