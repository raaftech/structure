"use strict";

const R = require('ramda');
const util = require('util');
const clc = require('cli-color');
const fs = require('fs');
const treeify = require('./treeify');
const uuidV4 = require('uuid');
const Structure = require('./subgraph');

const notNil = R.compose(R.not, R.isNil);

/*
    Generate a Inquirer compatible question to prompt the user with
    @param name The name of the property to store the input in
    @param type The type of question to ask
    @param ask String offered to the user, explaining the details of the question
    @param options Optional options to pass to the Inquirer prompt
 */
function generateQuestion(name, type, ask, options) {
	return R.merge(R.zipObj(['name','type','message'],[name,type,ask]), options);
}
/*
    Wrapper function to easily create a message as understood by structure
 */
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

function treePrint(verbose, entity) {
    let shortener = R.assocPath(['file','dataSchema','content'],'.....');
    let typecheck = R.ifElse(R.isArrayLike, R.map(shortener), shortener);

    if(!verbose) entity = typecheck(entity);
    return treeify.asTree(entity, true);
}
//TODO remove, obsolete
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

/*
    Construct an Entity object from normal json
    @param generateUuid Boolean, indicating if a new uuid should be generated
    @param json The json to transform to an Entity object.
 */
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

module.exports =  {
	generateQuestion: generateQuestion,
	createOptions: createOptions,
	getResultData: getResultData,
	getEventBusAddress: getEventBusAddress,
	getCommonFields: getCommonFields,
	stringify: stringify,
	consolidateComponents: consolidateComponents,
	readJsonFile: readJsonFile,
    treePrint: R.curry(treePrint),
    json2Entity: R.curry(json2Entity)
};
