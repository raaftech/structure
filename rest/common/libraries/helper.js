'use strict';

const R = require('ramda');
const uuidV4 = require('uuid');
const Structure = require('./subgraph');

const notNil = R.compose(R.not, R.isNil);

function createOptions(cmd, args) {
    return R.zipObj(['msgh', 'args'], [R.defaultTo('help', cmd), R.defaultTo({}, args)]);
}

function getEventBusAddress(initial, parts) {
    return R.join('.', R.prepend(initial, parts));
}

function getResultData(path, result) {
    //return R.path(['body', 'result', 'data'])(result);
    return R.path(path)(result);
}

function stringify(json) {
    return JSON.stringify(json, undefined, 2);
}

function entity2Graph(entity) {
    let container = {};
    container[entity.meta.uuid] = entity;
    let graph = new Structure.SubGraph(container);
    return graph;
}

function createInstance(baseTypeEntity, instanceEntity) {
    let rel = instanceEntity.relationCreate('INSTANCE_OF', 'outgoing');
    rel.addEntity(baseTypeEntity);
    let container = {};
    container[instanceEntity.meta.uuid] = instanceEntity;
    return container;
}

function json2Entity(generateUuid, json) {
    if(R.isNil(json)) return null;
    let defaultUuid = R.ifElse(R.identity, x => uuidV4(), x => R.defaultTo(null, json.meta.uuid))(generateUuid);
    let defaultRelations = R.ifElse(R.identity,
        x => R.ifElse(notNil, R.omit(['ISA']), R.identity)(R.defaultTo(null, R.clone(json.meta.relations))),
        x => R.defaultTo(null, R.clone(json.meta.relations)))(generateUuid);

    return new Structure.Entity(
        R.defaultTo(null, json.type),
        R.defaultTo(null, json.name),
        R.defaultTo(null, json.desc),
        defaultUuid,
        R.defaultTo(null, R.clone(json.data)),
        defaultRelations,
        R.defaultTo(null, R.clone(json.file))
    );
}

module.exports = {
    createOptions: R.curry(createOptions),
    getEventBusAddress: R.curry(getEventBusAddress),
    getResultData: R.curry(getResultData),
    stringify: stringify,
    entity2Graph: entity2Graph,
    createInstance: createInstance,
    json2Entity: json2Entity
};