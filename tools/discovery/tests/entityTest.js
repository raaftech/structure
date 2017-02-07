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

test('SubGraph construction (readBundle/toMap) tests', function(tt) {
    tt.test('core bundle load test', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        const result = sg.toMap();

        let check = R.map(isNested, R.values(result));
        let nested = R.gt(R.length(R.filter(R.equals(R.T()), check)),0);

        t.ok(R.is(Object, result), 'Result should be an object');
        t.ok(isNotAnArray(result), 'Result should not be an array');
        t.equals(Object.keys(result).length, 116, 'Should be 116 property keys');
        t.ok(nested,'Should contain nested items');
        t.end();
    });

    tt.test('Instantiation with invalid type string', function(t) {
        let sg = new entityLib.SubGraph("INVALID CONSTRUCTOR INPUT");
        const map = sg.toMap();
        t.ok(R.isEmpty(map), 'Should be an empty subgraph');
        t.end();
    });

    tt.test('Instantiation with invalid type number', function(t) {
        let sg = new entityLib.SubGraph(554489);
        const map = sg.toMap();
        t.ok(R.isEmpty(map), 'Should be an empty subgraph');
        t.end();
    });

    tt.test('Instantiation with invalid type Date', function(t) {
        let sg = new entityLib.SubGraph(new Date());
        const map = sg.toMap();

        t.ok(R.isEmpty(map), 'Should be an empty subgraph');
        t.end();
    });
});

test('flatten tests', function(tt) {
    tt.test('SubGraph.toList() core bundle test', function(t) {
        const sg = new entityLib.SubGraph(baseTypeJson);
        const bundle = sg.toMap();

        t.ok(R.is(Object, bundle), 'Result should be an object');
        t.ok(isNotAnArray(bundle), 'Result should not be an array');
        t.equals(Object.keys(bundle).length, 116, 'Should be 116 property keys');

        let result = sg.toList();
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
    tt.test('find Parser type by case-different name', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let result = sg.findEntity('parser');

        t.equals(result.name, 'Parser', 'Should be the Parser type');
        t.equals(result.meta.uuid, 'b5fa636c-1067-45ac-b027-ade2797bef6f', 'Should be uuid: b5fa636c-1067-45ac-b027-ade2797bef6f')
        t.end();
    });

    tt.test('find Parser type by uuid', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let result = sg.findEntity('b5fa636c-1067-45ac-b027-ade2797bef6f');

        t.equals(result.name, 'Parser', 'Should be the Parser type');
        t.equals(result.meta.uuid, 'b5fa636c-1067-45ac-b027-ade2797bef6f', 'Should be uuid: b5fa636c-1067-45ac-b027-ade2797bef6f')
        t.end();
    });

    tt.test('search for unknown type', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let result = sg.findEntity('foobar');
        t.equals(result, null, 'Should be a null value');
        t.end();
    });
});

test('insertEntity tests', function(tt) {
    tt.test('insert a new entity', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let entity = new entityLib.Entity('MyType','TestType','my desc.','234hklj-2k3jh4');
        let rootType = sg.findEntity('b1bc963a-19cc-4518-aecb-a35d028c49e3');

        sg.insertEntity(entity, 'ISA', 'outgoing', rootType);
        t.equals(sg.nodeCount(), 117, 'Should be 117 property keys');
        let createdNode = sg.findEntity('234hklj-2k3jh4');
        t.ok(notNil(createdNode), 'The newly created node should exist in the new subgraph');
        t.equals(createdNode.meta.relations.ISA.entities[0].meta.uuid,
                 'b1bc963a-19cc-4518-aecb-a35d028c49e3',
                 'Root type should be nested in the ISA relation');
        t.end();
    });

    tt.test('insert already existing entity', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let entity = sg.findEntity('parser');
        let rootType = sg.findEntity('b1bc963a-19cc-4518-aecb-a35d028c49e3');
        sg.insertEntity(entity, 'ISA', 'outgoing', rootType);

        t.equals(sg.nodeCount(), 116, 'Should still be 116 property keys');

        t.end();
    });

    tt.test('insert already entity with new uuid but existing name', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let entity = sg.findEntity('parser');
        let oldUuid = entity.meta.uuid;
        entity.meta.uuid = 'my-new-test-uuid';
        let rootType = sg.findEntity('b1bc963a-19cc-4518-aecb-a35d028c49e3');
        let newGraph = sg.insertEntity(entity, 'ISA', 'outgoing', rootType);

        t.equals(sg.nodeCount(), 116, 'Should still be 116 property keys');
        let createdNode = sg.findEntity('my-new-test-uuid');
        t.equals(createdNode, null, 'No new node should have been created');

        createdNode = sg.findEntity(oldUuid);
        t.ok(notNil(createdNode), 'old node should still be there.');

        t.end();
    });
});

test('removeEntity test', function(tt) {
    tt.test('test removal of non-dependency type', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let parser = sg.findEntity('parser');
        let newGraph = sg.removeEntity(parser.meta.uuid);

        t.equals(sg.nodeCount(), 115, 'Should be 115 property keys');
        let removed = sg.findEntity('parser');
        t.ok(removed === null, 'Entity should not be in new graph anymore');
        t.end();
    });

    tt.test('test removal of dependency type', function(t) {
        let sg = new entityLib.SubGraph(baseTypeJson);
        let parser = sg.findEntity('type');
        let newGraph = sg.removeEntity(parser.meta.uuid);

        t.equals(sg.nodeCount(), 116, 'Should be 116 property keys');
        let removed = sg.findEntity('parser');
        t.ok(removed !== null, 'Entity should still be in the new graph');
        t.end();
    });
});