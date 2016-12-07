"use strict";

const test = require('tape');
const lib = require('../ansibleLib');

test.only('setup tests', function (tt) {
    tt.test('localhost setup scan test', function (t) {
        lib.setup('localhost')
            .then((result) => {
                console.log(result);
                t.end();
            })
            .catch((ex) => {
                t.end(JSON.stringify(ex));
            });
    });

    tt.test('orient.raaf.local setup scan test', function (t) {
        lib.setup('172.17.1.62')
            .then((result) => {
                console.log(result);
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
