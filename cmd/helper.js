var R = require('ramda');

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

module.exports = {
	generateQuestion: generateQuestion,
	createOptions: createOptions,
	getResultData: getResultData,
	getEventBusAddress: getEventBusAddress,
	getCommonFields: getCommonFields,
	stringify: stringify
};
