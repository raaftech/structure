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
        t.equals(R.path(['meta','uuid'],e), '234hklj-2k3jh4', 'uuid should be 234hklj-2k3jh4');
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
        const result = entityLib.readBundle(baseTypeJson);
        //console.log(stringify(result));

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
        const bundle = entityLib.readBundle(baseTypeJson);

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

test('findEntity tests', function(tt) {
    const content = entityLib.readBundle(baseTypeJson);
    tt.test('find Parser type by case-different name', function(t) {
        let result = entityLib.findEntity(content, 'parser');

        t.equals(result.name, 'Parser', 'Should be the Parser type');
        t.equals(result.meta.uuid, 'b5fa636c-1067-45ac-b027-ade2797bef6f', 'Should be uuid: b5fa636c-1067-45ac-b027-ade2797bef6f')
        t.end();
    });

    tt.test('find Parser type by uuid', function(t) {
        let result = entityLib.findEntity(content, 'b5fa636c-1067-45ac-b027-ade2797bef6f');

        t.equals(result.name, 'Parser', 'Should be the Parser type');
        t.equals(result.meta.uuid, 'b5fa636c-1067-45ac-b027-ade2797bef6f', 'Should be uuid: b5fa636c-1067-45ac-b027-ade2797bef6f')
        t.end();
    });

    tt.test('search for unknown type', function(t) {
        let result = entityLib.findEntity(content, 'foobar');
        t.equals(result, null, 'Should be a null value');
        t.end();
    });
});

test('insertEntity tests', function(tt) {
    const subgraph = entityLib.readBundle(baseTypeJson);

    tt.test('insert a new entity', function(t) {
        let entity = new entityLib.Entity('MyType','TestType','my desc.','234hklj-2k3jh4');
        let rootType = entityLib.findEntity(subgraph, 'b1bc963a-19cc-4518-aecb-a35d028c49e3');

        let newGraph = entityLib.insertEntity(subgraph, entity, 'ISA', 'outgoing', rootType);
        t.equals(Object.keys(newGraph).length, 117, 'Should be 117 property keys');
        let createdNode = entityLib.findEntity(newGraph, '234hklj-2k3jh4');
        t.ok(notNil(createdNode), 'The newly created node should exist in the new subgraph');
        t.equals(createdNode.meta.relations.ISA.entities[0].meta.uuid,
                 'b1bc963a-19cc-4518-aecb-a35d028c49e3',
                 'Root type should be nested in the ISA relation');
        t.end();
    });

    tt.test('insert already existing entity', function(t) {
        let entity = entityLib.findEntity(subgraph, 'parser');
        let rootType = entityLib.findEntity(subgraph, 'b1bc963a-19cc-4518-aecb-a35d028c49e3');
        let newGraph = entityLib.insertEntity(subgraph, entity, 'ISA', 'outgoing', rootType);

        t.equals(Object.keys(newGraph).length, 116, 'Should still be 116 property keys');

        t.end();
    });

    tt.test('insert already entity with new uuid but existing name', function(t) {
        let entity = entityLib.findEntity(subgraph, 'parser');
        let oldUuid = entity.meta.uuid;
        entity.meta.uuid = 'my-new-test-uuid';
        let rootType = entityLib.findEntity(subgraph, 'b1bc963a-19cc-4518-aecb-a35d028c49e3');
        let newGraph = entityLib.insertEntity(subgraph, entity, 'ISA', 'outgoing', rootType);

        t.equals(Object.keys(newGraph).length, 116, 'Should still be 116 property keys');
        let createdNode = entityLib.findEntity(newGraph, 'my-new-test-uuid');
        t.equals(createdNode, null, 'No new node should have been created');

        createdNode = entityLib.findEntity(newGraph, oldUuid);
        t.ok(notNil(createdNode), 'old node should still be there.');

        t.end();
    });
});

test('removeEntity test', function(tt) {
    const subgraph = entityLib.readBundle(baseTypeJson);

    tt.test('test removal of non-dependency type', function(t) {
        let parser = entityLib.findEntity(subgraph, 'parser');
        let newGraph = entityLib.removeEntity(subgraph, parser.meta.uuid);

        t.equals(Object.keys(newGraph).length, 115, 'Should be 115 property keys');
        let removed = entityLib.findEntity(newGraph, 'parser');
        t.ok(removed === null, 'Entity should not be in new graph anymore');
        t.end();
    });

    tt.test('test removal of dependency type', function(t) {
        let parser = entityLib.findEntity(subgraph, 'type');
        let newGraph = entityLib.removeEntity(subgraph, parser.meta.uuid);

        t.equals(Object.keys(newGraph).length, 116, 'Should be 116 property keys');
        let removed = entityLib.findEntity(newGraph, 'parser');
        t.ok(removed !== null, 'Entity should still be in the new graph');
        t.end();
    });
});