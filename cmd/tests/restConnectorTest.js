"use strict";

const test = require('tape');
const path = require('path');
const R = require('ramda');
const rest = require(path.join(__dirname, '..', 'restConnector'));

const notNil = R.compose(R.not, R.isNil);
const HOST = 'localhost';
const PORT = 3000;
const EMAIL = 'orchid@blackorchid.nl';
const PWD = 'test';

test('getEntityByName test', function(tt) {
    tt.test('Get known entity by name', function(t) {
        rest.getEntityByName(HOST, PORT, 'Computer')
            .then(result => {
                t.ok(notNil(result), 'there should be a result');
                t.ok(R.is(Object, result), 'should be an object');
                let conditions = [R.has('name'), R.has('meta'), R.has('type')];
                t.ok(R.allPass(conditions)(result), 'Should have Entity properties');
                t.end();
            })
            .catch(ex => t.end(ex));
    });

    tt.test('Get UNknown entity by name', function(t) {
        rest.getEntityByName(HOST, PORT, 'FOOBAR')
            .then(result => {
                t.ok(notNil(result), 'there should be a result');
                t.ok(R.is(Object, result), 'should be an object');
                t.ok(R.has('error',result), 'Should have error property set');
                t.equal(R.path(['error', 'status'],result), 500, 'Should be status code 500');
                t.end();
            })
            .catch(ex => t.end(ex));
    });
});

test('getEntityByUuid test', function(tt) {
    tt.test('Get known entity by uuid', function(t) {
        rest.getEntityByUuid(HOST, PORT, 'ec11db91-6973-4285-b018-b461fbb432d7')
            .then(result => {
                t.ok(R.isArrayLike(result), 'Should be array type');
                t.ok(notNil(R.head(result)), 'there should be a result');
                t.ok(R.is(Object, R.head(result)), 'should be an object');
                let conditions = [R.has('name'), R.has('meta'), R.has('type')];
                t.ok(R.allPass(conditions)(R.head(result)), 'Should have Entity properties');
                t.end();
            })
            .catch(ex => t.end(ex));
    });

    tt.test('Get UNknown entity by uuid', function(t) {
        rest.getEntityByUuid(HOST, PORT, 'dd11db91-6973-4285-b018-b461fbb432d7')
            .then(result => {
                t.ok(notNil(result), 'there should be a result');
                t.ok(R.is(Object, result), 'should be an object');
                t.ok(R.isEmpty(result), 'Should be empty object');
                t.end();
            })
            .catch(ex => t.end(ex));
    });
});

test('getChildren test', function(tt) {
    tt.test('get children for valid entity uuid', function(t) {
        rest.getChildren(HOST, PORT, 'ee92c308-80f4-40b2-af3a-77d26cd99517')
            .then(children => {
                t.ok(R.isArrayLike(children), 'Should be array type');
                t.ok(notNil(R.head(children)), 'there should be a result');
                t.ok(R.is(Object, R.head(children)), 'should be an object');
                let conditions = [R.has('name'), R.has('meta'), R.has('type')];
                t.ok(R.allPass(conditions)(R.head(children)), 'Should have Entity properties');
                t.end();
            })
            .catch(ex => t.end(ex));
    });

    tt.test('get children for INvalid entity uuid', function(t) {
        rest.getChildren(HOST, PORT, 'dd92c308-80f4-40b2-af3a-77d26cd99517')
            .then(children => {
                t.ok(R.is(Object, children), 'should be an object');
                t.ok(R.has('error', children), 'Should have error property');
                let conditions = [R.has('status'), R.has('message'), R.has('failureCode'),R.has('failureType')];
                t.ok(R.allPass(conditions)(children.error), 'Should have Error properties');
                t.equal(children.error.status,500, 'should be status 500');
                t.equal(children.error.failureCode, -1, 'should be failureCode -1');
                t.equal(children.error.failureType, 'NO_HANDLERS', 'should be failureType NO_HANDLERS');
                t.end();
            })
            .catch(ex => t.end(ex));
    });
});

test('lastInstances test', function(tt) {
    tt.test('get last 5 instances', function(t) {
        rest.lastInstances(HOST, PORT, 5)
            .then(instances => {
                t.ok(R.isArrayLike(instances), 'Should be array type');
                t.equal(R.length(instances), 5, 'Should be five entries');
                t.end();
            })
            .catch(ex => t.end(ex));
    });
});

test('getInstanceByUuid test', function(tt) {
    tt.test('get an instance by uuid', function(t) {
        rest.lastInstances(HOST, PORT, 1)
            .then(instances => R.pipe(R.head, R.path(['meta','uuid']))(instances))
            .then(uuid => rest.getInstanceByUuid(HOST, PORT, uuid))
            .then(instance => {
                t.ok(notNil(instance), 'there should be a result');
                t.ok(R.is(Object, instance), 'should be an object');
                let conditions = [R.has('name'), R.has('meta'), R.has('type')];
                t.ok(R.allPass(conditions)(instance), 'Should have Entity properties');
                t.end();
            })
            .catch(ex => t.end(ex));
    });
});

test('createInstance test', function(tt) {
    tt.test('create instance with valid token', function(t) {
        rest.login(HOST, PORT, EMAIL, PWD)
            .then(user => user.id)
            .then(token => {
                let options = {
                    name: 'discovery',
                    node: 'ec11db91-6973-4285-b018-b461fbb432d7',
                    desc: 'some description'
                };
                return rest.createInstance(HOST, PORT, token, options);
            })
            .then(result => {
                t.ok(R.isEmpty(result), 'Should be empty object');
                t.end();
            })
            .catch(ex => t.end(ex));
    });

    tt.test('use invalid token', function(t) {
        let options = {
            name: 'discovery',
            node: 'ec11db91-6973-4285-b018-b461fbb432d7',
            desc: 'some description'
        };
        rest.createInstance(HOST, PORT, 'invalid-token', options)
            .then(result => {
                t.ok(notNil(result), 'there should be a result');
                t.ok(R.is(Object, result), 'should be an object');
                t.ok(R.has('error',result), 'Should have error property');
                let conditions = [R.has('name'),R.has('status'),R.has('message'),R.has('statusCode'),R.has('code')];
                t.ok(R.allPass(conditions)(result.error), 'Should have all create error properties');
                t.equal(result.error.status, 401, 'Should be 401 status');
                t.equal(result.error.statusCode, 401, 'Should be 401 statusCode');
                t.equal(result.error.code,'AUTHORIZATION_REQUIRED', 'Should be code AUTHORIZATION_REQUIRED');
                t.equal(result.error.message, 'Authorization Required', 'Should be message Authorization Required');
                t.end();
            })
            .catch(ex => t.end(ex));
    });
});

test.skip('runInstrument test', function(tt) {

});

test.skip('login test', function(tt) {
    tt.test('Login', function(t) {
        rest.login(HOST, PORT, EMAIL, PWD)
            .then(user => {
                t.ok(notNil(user), 'there should be a result');
                t.ok(R.is(Object, user), 'should be an object');
                let conditions = [R.has('id'), R.has('ttl'), R.has('created'), R.has('userId')];
                t.ok(R.allPass(conditions)(user), 'Should have Login properties');
                t.end();
            })
            .catch(ex => t.end(ex));
    });
});

test.skip('logout test', function(tt) {
});

test.skip('createUser test', function(tt) {
    tt.test('createUser test', function(t) {
        rest.createUser(HOST, PORT, EMAIL, PWD)
            .then(user => {
                t.ok(notNil(user), 'there should be a result');
                t.ok(R.is(Object, user), 'should be an object');
                let conditions = [R.has('email'), R.has('id')];
                t.ok(R.allPass(conditions)(user), 'Should have User properties');
                t.equal(user.email, EMAIL, 'Should be requested email');
                t.end();
            })
            .catch(ex => t.end(ex));
    })
});