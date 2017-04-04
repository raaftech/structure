'use strict';

const R = require('ramda');

function createOptions(cmd, args) {
    return R.zipObj(['msgh', 'args'], [R.defaultTo('help', cmd), R.defaultTo({}, args)]);
}

function getEventBusAddress(initial, parts) {
    //return R.join('.', R.prepend('com.raaftech.structure', parts));
    return R.join('.', R.prepend(initial, parts));
}

function getResultData(path, result) {
    //return R.path(['body', 'result', 'data'])(result);
    return R.path(path)(result);
}

function stringify(json) {
    return JSON.stringify(json, undefined, 2);
}

module.exports = {
    createOptions: R.curry(createOptions),
    getEventBusAddress: R.curry(getEventBusAddress),
    getResultData: R.curry(getResultData),
    stringify: stringify
};