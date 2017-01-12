"use strict";

const test = require('tape');
const R = require('ramda');
const fs = require('fs');
const entityLib = require('../entity');

const stringify = (json) => JSON.stringify(json, undefined, 2);
const isNotAnArray = R.compose(R.not, R.isArrayLike);
const notNil = R.compose(R.not, R.isNil);
const baseTypeJson = JSON.parse(fs.readFileSync('base.type.json', {encoding: 'utf8'}));
const isNested = (obj) => {
    let rels = R.values(R.path(['meta','relations'],obj));
    let check = R.map((x) => notNil(R.prop('entities',x)), rels);
    let res = R.filter((a) => a !== false, check);
    return R.gt(R.length(res), 0);
};

test('Entity class tests', function(tt) {
    tt.test('construction test', function(t) {
        let e = new entityLib.Entity('Component','TestComponent','my desc.','234hklj-2k3jh4');
        t.ok(R.has('type',e), 'should have type property');
        t.equals(R.prop('type',e), 'Component', 'type value should be Component');
        t.ok(R.has('name',e), 'should have name property');
        t.equals(R.prop('name',e), 'TestComponent', 'name value should be TestComponent');
        t.ok(R.has('desc',e), 'should have desc property');
        t.equals(R.prop('desc',e), 'my desc.', 'desc value should be "my desc."');
        t.ok(R.has('meta',e), 'should have meta property');
        t.equals(R.path(['meta','uuid'],e), '234hklj-2k3jh4', 'uuid should be 234hklj-2k3jh4')
        t.end();
    });

    tt.test('create relation, default direction test', function(t) {
        let e = new entityLib.Entity('Component','TestComponent','my desc.','234hklj-2k3jh4');
        e.relationCreate('IMPLEMENTS');
        t.equals(e.meta.relations['IMPLEMENTS'].direction, 'outgoing', 'Should be outgoing IMPLEMENTS relation');
        t.end();
    });

    tt.test('create relation test', function(t) {
        let e = new entityLib.Entity('Component','TestComponent','my desc.','234hklj-2k3jh4');
        e.relationCreate('ISA', 'incoming');
        t.equals(e.meta.relations['ISA'].direction, 'incoming', 'Should be incoming ISA relation');
        t.end();
    });
});

test('readBundle tests', function(tt) {
    tt.test('core bundle test', function(t) {
        let content = baseTypeJson;
        let result = entityLib.readBundle(content);

        let check = R.map(isNested, R.values(result));
        let nested = R.gt(R.length(R.filter(R.equals(R.T()), check)),0);

        t.ok(R.is(Object, result), 'Result should be an object');
        t.ok(isNotAnArray(result), 'Result should not be an array');
        t.equals(Object.keys(result).length, 116, 'Should be 116 property keys');
        t.ok(nested,'Should contain nested items');
        t.end();
    });
});

test('flatten tests', function(tt) {
    tt.test('flatten core bundle test', function(t) {
        let content = baseTypeJson;
        let bundle = entityLib.readBundle(content);

        t.ok(R.is(Object, bundle), 'Result should be an object');
        t.ok(isNotAnArray(bundle), 'Result should not be an array');
        t.equals(Object.keys(bundle).length, 116, 'Should be 116 property keys');

        let result = entityLib.flatten(bundle);
        let check = R.map(isNested, result);
        let nested = R.equals(R.length(R.filter(R.equals(R.T()), check)),0);

        t.ok(nested, 'Flattened result should have no nested entries');
        t.ok(R.isArrayLike(result), 'Flattened result should be an array');
        t.equals(result.length,Object.keys(bundle).length,
            'Flattened array length should be the same as the number of keys in the bundle hierarchy');
        t.end();
    });
});
