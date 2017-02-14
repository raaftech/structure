"use strict";

const R = require('ramda');
const util = require('util');
const clc = require('cli-color');
const fs = require('fs');

function generateQuestion(name, type, ask, options) {
	return R.merge(R.zipObj(['name','type','message'],[name,type,ask]), options);
}

function createOptions(cmd, args) {
	return R.zipObj(['msgh','args'],[R.defaultTo('help',cmd), R.defaultTo({}, args)]);
}

function getResultData(result) {
	return R.path(['body','result','data'])(result);
}

function stringify(json) {
    return JSON.stringify(json, undefined, 2);
}

function getEventBusAddress(parts) {
    return R.join('.',R.prepend('com.raaftech.structure', parts));
}

function getCommonFields() {
    return ['isa','schema','name','desc','uuid'];
}

function consolidateComponents(uuid, componentList) {
    let result = {
        uuid: uuid
    };
    R.forEach((elem) => {
        if(elem.name === 'Ipv4Address') {
            result[elem.name] = R.head(R.match(/([0-9]{1,3}\.?){4}/g, elem.desc));
        } else {
            if(elem.desc === 'unknown') result[elem.name] = clc.red(elem.desc);
            else result[elem.name] = elem.desc;
        }
    }, componentList);
    return result;
}

function readJsonFile(filePath) {
    return JSON.parse(fs.readFileSync(filePath, {encoding: 'utf8'}));
}

module.exports = {
	generateQuestion: generateQuestion,
	createOptions: createOptions,
	getResultData: getResultData,
	getEventBusAddress: getEventBusAddress,
	getCommonFields: getCommonFields,
	stringify: stringify,
	consolidateComponents: consolidateComponents,
	readJsonFile: readJsonFile
};
