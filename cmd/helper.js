"use strict";

const R = require('ramda');
const fs = require('fs');
const treeify = require('./treeify');

/*
    Wrapper function to handle calls to endpoints that have an ACL set.
    @param done The done() function from vorpal
    @param loggedInFn The function to run if logged in
    @param token The token for the REST service
 */
const genericAuthCommandHandler = R.curry((done, loggedInFn, token) => {
    let loggedOut = () => { console.log('Please login first.'); done();};
    let handler = R.ifElse(R.isNil,loggedOut, loggedInFn);
    handler(token);
});
/*
    Wrapper function to generically handle promise catch entries
    @param done The done() function from vorpal
    @param error The error object caught in the catch block
 */
const genericCatchHandler = R.curry((done, error) => {
    console.log(error);
    done();
});
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
    return R.join('.',R.prepend('tech.raaf.structure.bus', parts));
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

function readJsonFile(filePath) {
    return JSON.parse(fs.readFileSync(filePath, {encoding: 'utf8'}));
}

module.exports =  {
	generateQuestion: generateQuestion,
	createOptions: createOptions,
	getResultData: getResultData,
	getEventBusAddress: getEventBusAddress,
	getCommonFields: getCommonFields,
	stringify: stringify,
	readJsonFile: readJsonFile,
    treePrint: R.curry(treePrint),
    genericAuthCommandHandler: genericAuthCommandHandler,
    genericCatchHandler: genericCatchHandler,
    notNil: notNil
};
