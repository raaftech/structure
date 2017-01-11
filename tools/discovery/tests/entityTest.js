"use strict";

const test = require('tape');
const R = require('ramda');
const fs = require('fs');
const entityLib = require('../entity');

const stringify = (json) => JSON.stringify(json, undefined, 2);

test('Entity class tests', function(tt) {
    tt.test('construction test', function(t) {
        let e = new entityLib.Entity('Component','TestComponent','my desc.','234hklj-2k3jh4');
        console.log(stringify(e));
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
        let content = JSON.parse(fs.readFileSync('base.type.json', {encoding: 'utf8'}));
        let result = entityLib.readBundle(content);
        t.equals(Object.keys(result).length, 116, 'Should be 116 property keys');
        t.end();
    });
});

test('flatten tests', function(tt) {
    tt.test('flatten core bundle test', function(t) {
        let nested = JSON.parse(fs.readFileSync('base.type.json', {encoding: 'utf8'}));
        let bundle = entityLib.readBundle(nested);
        let result = entityLib.flatten([bundle]);

        t.equals(result.length,Object.keys(bundle).length,
            'Flattened array length should be the same as the number of keys in the bundle hierarchy');
        t.end();
    });
});