"use strict";
const vorpal = require('vorpal')();
const R = require('ramda');
const util = require('util');
const helper = require('./helper');
const EventBus = require('vertx3-eventbus-client');
const clc = require('cli-color');
const sprintf = require('sprintf-js').sprintf;
const treeify = require('treeify');
const neo = require('./extensions/neo');
const fs = require('fs');
const path = require('path');
const RestConnector = require('./restConnector');

let rest = new RestConnector('localhost', 3000);
let eventBusUrl = '';
let eb = null;
init();


function init() {
	eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
	eb = new EventBus(eventBusUrl);
	wrapper().then((res) => {
		console.log(res);
		setupStatusChannel();
		scli();
	});
}

function wrapper() {
	return new Promise((resolve,reject) => {
		eb.onopen = () => resolve(clc.green.bold(util.format('Connected to: %s', eventBusUrl)));

		eb.onclose = (error) => resolve(clc.red.bold(util.format('Error while connecting to [%s]: %s', eventBusUrl, error.reason)));
	});
}

function scli() {
	vorpal.history('structure-history');
	vorpal.use(neo);
	vorpal.delimiter(clc.magenta.bold('structure$')).show();
}

function setupStatusChannel() {
    eb.registerHandler('com.raaftech.structure.status',function(error, message) {
        if(error) {
            console.log("ERROR: "+error);
        } else {
            let uuid = message.body.data.uuid;
            if(!R.isNil(uuid)) {
                sendEvent(helper.getEventBusAddress(['repository','types']),'findOne',{node: uuid})
                    .then((data) => {
                        console.log(helper.stringify(data));
                        let comps = helper.getResultData(data)["components"];
                        comps = helper.consolidateComponents(clc.green(uuid), comps);
                        let name = helper.getResultData(data)['name'];
                        if(name.indexOf('unknown-') !== -1) name = clc.red(name);
                        else name = clc.green(name);
                        let tree = {};
                        tree[name] = comps;
                        console.log(treeify.asTree(tree, true));
                    });
            }
        }
    });
}

function sendEvent(base, cmd, args) {
	let options = helper.createOptions(cmd,args);
	return new Promise((resolve,reject) => {
		eb.send(base, options, (error, result) => {
			if(error) reject(error);
			else resolve(result);
		});
	});
}

function getChildren(uuid) {
    return new Promise((resolve, reject) => {
        sendEvent(helper.getEventBusAddress(['node','type',uuid]),'getSubtypes',{})
            .then((data) => {
                let response = R.project(['name','uuid'], helper.getResultData(data));
                resolve(response);
            });
    });
}

function toRoot(uuid, childNodes) {
    return new Promise((resolve, reject) => {
        sendEvent(helper.getEventBusAddress(['repository', 'nodes']), 'findOne', {'node': uuid})
            .then((parent) => {
                parent = helper.getResultData(parent);
                let tree = {};
                tree[parent.name] = childNodes;
                if(R.isNil(parent.isa)) {
                    resolve(tree);
                } else {
                    resolve(toRoot(parent.isa, tree));
                }
            });
    });
}

function payloadDir() {
    return path.join(__dirname, 'payloads');
}

function getPayloadListing() {
    return fs.readdirSync(payloadDir());
}


//commands
//TODO extract commands to extension sets

vorpal.command('connect', 'Connect to the eventbus')
    .action(function(args, done) {
        this.log('Connecting to '+clc.green.bold(eventBusUrl));
        eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
        eb = new EventBus(eventBusUrl);
        wrapper().then((res) => {
            console.log(res);
            done();
        });
    });

vorpal.command('disconnect', 'Disconnect from the EventBus')
    .action(function(args, done) {
        this.log(clc.red(util.format('Closing connection to EventBus (%s)', eventBusUrl)));
        eb.close();
        this.log(clc.red.bold('Connection closed.'));
        done();
    });

vorpal.command('find <name>', 'Find a specified node in vertx')
    .option('-f, --field <fieldName>', 'Select a specific field from the resultset',
        helper.getCommonFields())
    .option('-u, --uuid', 'Set search mode to uuid based search.')
    .action(function(args, done) {
        const self = this;

        let handler = (method, arg) => {
            rest[method](arg)
                .then(entity => {
                    self.log(helper.stringify(entity));
                    let meta = R.curry((ent,list) => {
                        return R.pipe(
                            R.prop('meta'),
                            R.keys,
                            R.map(item => 'meta.'+item),
                            R.concat(list)
                        )(ent);
                    });
                    let keys = R.pipe(R.keys, R.prepend('ALL'),meta(entity))(entity);
                    self.prompt(helper.generateQuestion('list', 'list', 'Choose a field', {choices: keys}), function(result) {
                        if(result.list === 'ALL') self.log(helper.stringify(entity));
                        else {
                            let pp = R.split('.',result.list);
                            let prop = clc.green.bold(result.list);
                            self.log(util.format('%s: %s',prop,helper.stringify(R.path(pp, entity))));
                        }
                        done();
                    });
                })
                .catch(ex => {
                    self.log(ex);
                    done();
                });
        };

        if(args.options.uuid) {
            handler('getEntityByUuid', args.name);
        } else {
            handler('getEntityByName', args.name);
        }
    });

vorpal.command('instance')
    .option('-l, --last <quantity>', 'Fetch the last (quantity) created instances')
    .option('-f, --find <name>', 'Find a specific instance by name')
    .action(function(args, done) {
        const self = this;
        let act = args.options.last ? 'nLastCreated' : 'findOneByName';
        let pargs = args.options.last ? {'amount': args.options.last} : {'name': args.options.find};
        let addr = ['repository', 'instances'];
        sendEvent(helper.getEventBusAddress(addr), act, pargs)
            .then((data) => {
                let databuf = helper.getResultData(data);
                self.log(helper.stringify(databuf));
                done();
            });
    });

vorpal.command('dig <uuid>', 'Get the child elements for a node')
    .action(function(args, done) {
        const self = this;
        sendEvent(helper.getEventBusAddress(['node','type',args.uuid]),'getSubtypes',{})
            .then((data) => {
                let response = R.project(['name','uuid'], helper.getResultData(data));
                R.forEach((item) => {
                    self.log(sprintf('%-16s: %24s', item.name,item.uuid));
                }, response);
                done();
            });
    });


vorpal.command('show <uuid>', 'Show a node by its uuid.')
    .option('-f, --field <fieldName>', 'Select a specific field from the resultset',
        ['getPath', 'help','version','uuid'])
    .action(function(args, done) {
        const self = this;
        sendEvent(helper.getEventBusAddress(['node','type',args.uuid]), 'help', {})
            .then((data) => {
                if(args.options.field) {
                    self.log(helper.stringify(data.body.result[args.options.field]));
                    done();
                } else {
                    let databuf = data.body.result;
                    let keys = R.pipe(R.keys, R.prepend('ALL'))(databuf);
                    self.prompt(helper.generateQuestion('list', 'list', 'Choose a field', {choices: keys}), function(result) {
                        if(result.list === 'ALL') self.log(helper.stringify(databuf));
                        else {
                            let prop = clc.green.bold(result.list);
                            self.log(util.format('%s: %s',prop,helper.stringify(databuf[result.list])));
                        }
                        done();
                    });
                }
            });
    });


vorpal.command('tree <name>', 'Show a relation tree for the requested node')
    .option('-u, --uuid', 'Make the search go by uuid instead of name')
    .action(function(args, done) {
        const self = this;
        let act = args.options.uuid ? 'findOne' : 'findOneByName';
        let pargs = args.options.uuid ? {'node': args.name} : {'name': args.name};
        sendEvent(helper.getEventBusAddress(['repository', 'types']), act, pargs)
            .then((data) => {
                let databuf = helper.getResultData(data);
                let tree = {};
                getChildren(databuf.uuid)
                    .then((children) => {
                        R.forEach((item) => {
                            tree[item.name] = item.uuid;
                        }, children);

                        toRoot(databuf.uuid, tree)
                            .then((parents) => {
                                self.log(treeify.asTree(parents,true));
                                done();
                            });
                    });
            });
    });


vorpal.command('discover <cidr>', 'Discover items in the network')
    .action(function(args, done) {
        const self = this;
        let pargs = {
            node: '299001a6-1091-4c0d-87be-c512af03e179',
            name: 'testname',
            checksum: 'foobar',
            addr: args.cidr,
            reachable: true,
            desc: 'Some cidr description.'
        };
        sendEvent(helper.getEventBusAddress(['repository', 'instances']), 'putNode', pargs)
            .then((data) => {
                console.log(data);
                let uuid = data.body.result.data;
                self.log('instance uuid: '+uuid);
                sendEvent(helper.getEventBusAddress(['node', 'component',uuid]), 'runInstrument', {
                    instrument: 'IpAddressDiscover',
                    timeAllocated: 600000
                }).then((result) => {
                    done();
                });
            });
    });

vorpal.command('query', 'Place arbitrary requests on the event bus')
    .action(function(args,done) {
        const self = this;
        let qs = [];
        qs.push(helper.generateQuestion('address', 'input', 'Event bus address: '));
        qs.push(helper.generateQuestion('cmd', 'input', 'Command (msgh): '));
        qs.push(helper.generateQuestion('file','list','Select payload file: ',{choices: getPayloadListing()}));

        self.prompt(qs, function(res) {
            let json = helper.readJsonFile(payloadDir() + '/' + res.file);
            sendEvent(res.address,res.cmd,json)
                .then((data) => {
                    console.log(helper.stringify(data));
                    done();
                })
                .catch((ex) => {console.log(ex); done();});
        });
    });