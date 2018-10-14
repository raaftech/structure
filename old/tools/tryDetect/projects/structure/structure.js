'use strict';

const R = require('ramda');
const RestConnector = require('./restConnector');
const graphLib = require('./graphLib');

// {
//	db: [],
//  token: '',
//  host: '',
//  port: 3000
// 	existingInstance: Object,
// 	requestedType: "IpAddress",
// 	relation: 'HAS',
// 	data: {}
// }
const createGenericInstance = (input) => {
    let requestedType = R.find(R.propEq('name', input.requestedType), input.db);
    let typeInstance = graphLib.createInstance(requestedType, input.data);
    let pair = graphLib.relateEntity(input.existingInstance, 'HAS', 'outgoing', typeInstance);

	let newUuid = R.last(pair).meta.uuid;
	let rest = new RestConnector(input.host, input.port);
	rest.setToken(input.token);

	return rest.createInstance({entities: [R.last(pair)]})
		.then(result => {
			console.log('----->createInstance result: '+JSON.stringify(result));
			return rest.updateInstance({entities: [R.head(pair)]})
		})
		.then(result => {
			console.log('----->updateInstance result: '+JSON.stringify(result));
			return newUuid;
		});
};

module.exports = {
	createGenericInstance: createGenericInstance
};
