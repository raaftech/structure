"use strict";

const R = require('ramda');
const rc = require('./restConnector');

const HOST = 'localhost';
const PORT = 3000;

// rc.getEntityByName('localhost',3000, 'Component')
// 	.then((result) => console.log(result))
// 	.catch(ex => console.log(ex));

rc.getEntityByUuid('localhost', 3000, 'a79ce509-a021-4177-ae61-73abd01fb479')
	.then(result => console.log(result))
	.catch(ex => console.log(ex));

// let data = {
// 	node: '299001a6-1091-4c0d-87be-c512af03e179',
// 	name: 'testnet',
// 	checksum: 'foobar',
// 	addr: '172.17.1.0/24',
// 	reachable: true,
// 	desc: 'Some test-cidr description.'
// };
//
// rc.createInstance(HOST, PORT, {args: JSON.stringify(data)})
// 	.then((result) => {
// 		console.log('uuid: '+result);
// 		return rc.discover(HOST, PORT, result);
// 	})
// 	.then((disco) => console.log(disco))
// 	.catch((ex) => console.log(ex));
