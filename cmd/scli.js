"use strict";
const vorpal = require('vorpal')();
const R = require('ramda');
const util = require('util');
const helper = require('./helper');
const clc = require('cli-color');
const sprintf = require('sprintf-js').sprintf;
const treeify = require('./treeify');
const fs = require('fs');
const path = require('path');
const RestConnector = require('./restConnector');
const SchemaValidator = require('./SchemaValidator');
const SimpleCache = require('./simpleCache');
const EventSource = require('eventsource');
const Structure = require('./subgraph');
//extensions
const neoExt = require('./extensions/neo');
const structureExt = require('./extensions/structure');
const groupExt = require('./extensions/groups');
const instanceExt = require('./extensions/instance');

const answerCache = new SimpleCache();
let rest;
let validator;

//Program entry point
init();

function init() {
    rest = new RestConnector(
        R.defaultTo('localhost', process.env.RESTHOST),
        R.defaultTo(3000, process.env.RESTPORT)
    );
    validator = new SchemaValidator(R.defaultTo('http://localhost:8080/schemas/', process.env.SCHEMAHOST));
    answerCache.init();

    setupStatusChannel();
    scli();
}

function scli() {
	vorpal.history('structure-history');
	vorpal.use(neoExt, answerCache);
	vorpal.use(structureExt, rest);
	vorpal.use(groupExt, rest);
	vorpal.use(instanceExt, {rest: rest, validator: validator});
	vorpal.delimiter(clc.magenta.bold('structure$')).show();
}

function setupStatusChannel() {
    const changeUrl = rest.getChangeStreamUrl();
    const es = new EventSource(changeUrl);
    es.addEventListener('data', function(message) {
        let data = JSON.parse(message.data);
        console.log(JSON.stringify(data, undefined, 2));
    });
}

function getChildren(uuid) {
    return rest.getChildren(uuid)
        .then(children => {
            return R.map((item) => {
                    return R.zipObj(['name', 'uuid'],[R.prop('name',item), R.path(['meta','uuid'], item)]);
                }, children);
        });
}

function toRoot(uuid, childNodes) {
    return new Promise((resolve, reject) => {
        rest.getEntityByUuid(uuid)
            .then(parent => {
                let tree = {};
                tree[parent.name] = childNodes;
                if(R.isNil(R.path(['meta','relations','ISA'], parent))) {
                    resolve(tree);
                } else {
                    //getIsaParent: small function to fetch the first of the ISA uuids for an entity
                    let getIsaParent = R.pipe(R.path(['meta','relations','ISA','uuids']), R.head);
                    resolve(toRoot(getIsaParent(parent), tree));
                }
            })
    });
}

function getDomainClass(uuid) {
    return new Promise((resolve, reject) => {
        toRoot(uuid, {})
            .then(parents => {
                let fn = R.pipe(R.prop('Type'),R.keys,R.head);
                resolve(fn(parents));
            })
            .catch(ex => reject(ex));
    });
}

function payloadDir() {
    return path.join(__dirname, 'payloads');
}

function getPayloadListing() {
    return fs.readdirSync(payloadDir());
}

vorpal.command('find <name>', 'Find a specified node in structure')
    .option('-f, --field <fieldName>', 'Select a specific field from the resultset',
        helper.getCommonFields())
    .option('-u, --uuid', 'Set search mode to uuid based search.')
    .option('-v, --verbose', 'Be verbose with the result output')
    .action(function(args, done) {
        const self = this;
        //Function that calls a specific rest method, depending on the value of the method argument
        let handler = (method, arg) => {
            rest[method](arg)
                .then(entity => {
                    //Function that fetches the property keys, present in the meta object in the entity
                    //Then adds the property path, in dot notation, to the list (2nd arg)
                    //Returns a list of possible property paths
                    let meta = R.curry((ent,list) => {
                        return R.pipe(R.prop('meta'), R.keys,
                            R.map(item => 'meta.'+item),
                            R.concat(list)
                        )(ent);
                    });
                    //Function that gets all keys from an entity, prepends 'ALL' to the list
                    //fetches the meta keys by using above function and appends them to the list
                    //This list is then prompted to the user to choose from.
                    let keys = R.pipe(R.keys, R.prepend('ALL'),meta(entity))(entity);
                    self.prompt(helper.generateQuestion('list', 'list', 'Choose a field', {choices: keys}), function(result) {
                        let check = R.ifElse(R.propEq('list','ALL'),
                                        () => self.log(helper.treePrint(args.options.verbose, entity)),
                                        () => {
                                            let pp = R.split('.',result.list);
                                            let prop = clc.green.bold(result.list);
                                            let outputTypeSelector = R.ifElse(R.is(Object),
                                                x => self.log(util.format('%s\n%s', prop, helper.treePrint(args.options.verbose,R.path(pp, entity)))),
                                                x => self.log(util.format('%s: %s', prop, helper.stringify(R.path(pp, entity)))));
                                            outputTypeSelector(R.path(pp, entity));
                                        }
                                    );
                        check(result);
                        done();
                    });
                })
                .catch(helper.genericCatchHandler(done));
        };

        //Function that determines which rest call to use based on the given arguments and options
        let exec = R.ifElse(R.isNil,
            () => handler('getEntityByName', args.name),
            () => handler('getEntityByUuid', args.name));
        exec(R.path(['options', 'uuid'], args));
    });

vorpal.command('dig', 'Get the child elements for a type-node')
    .option('-n, --name <name>', 'Identify the type-node by name')
    .option('-u, --uuid <uuid>', 'Identify the type-node by uuid')
    .action(function(args, done) {
        const self = this;

        if(args.options.name) {
            rest.getEntityByName(args.options.name)
                .then(entity => getChildren(entity.meta.uuid))
                .then(children => {
                    R.forEach((item) => {
                        self.log(sprintf('%-16s: %24s', R.prop('name', item),R.prop('uuid',item)));
                    }, children);
                    done();
                })
                .catch(helper.genericCatchHandler(done));
        } else {
            getChildren(args.options.uuid)
                .then(children => {
                    R.forEach((item) => {
                        self.log(sprintf('%-16s: %24s', R.prop('name', item),R.prop('uuid',item)));
                    }, children);
                    done();
                })
                .catch(helper.genericCatchHandler(done))
        }
    });

vorpal.command('tree <name>', 'Show a relation tree for the requested node')
    .option('-u, --uuid', 'Make the search go by uuid instead of name')
    .action(function(args, done) {
        const self = this;
        let rootUuid = '';
        //Function that calls a specific rest method, depending on the value of the method argument
        let handler = (method, arg) => {
            rest[method](arg)
                .then(entity => { return R.path(['meta','uuid'], entity) })
                .then(uuid => { rootUuid = uuid; return getChildren(uuid); })
                .then(children => {
                    let tree = {};
                    R.forEach((item) => {
                        tree[item.name] = item.uuid;
                    }, children);

                    return tree;
                })
                .then(tree => {
                    return toRoot(rootUuid, tree);
                })
                .then(parents => {
                    self.log(treeify.asTree(parents,true));
                    done();
                })
                .catch(helper.genericCatchHandler(done));
        };

        //Function that determines which rest call to use based on the given arguments and options
        let exec = R.ifElse(R.isNil,
            () => handler('getEntityByName', args.name),
            () => handler('getEntityByUuid', args.name));
        exec(R.path(['options', 'uuid'], args));
    });

//TODO finish refactor
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
        //function to run if logged in
        let loggedIn = () => rest.createInstance(pargs)
            .then(created => {
                let instanceUuid = created.meta.uuid;
                return rest.runInstrument('IpAddressDiscover', instanceUuid);
            })
            .then(result => done())
            .catch(helper.genericCatchHandler(done));

        helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
    });

vorpal.command('query', 'Place arbitrary requests on the event bus')
    .action(function(args,done) {
        const self = this;
        let qs = [];
        qs.push(helper.generateQuestion('address', 'input', 'Event bus address: ', {default: answerCache.get('query', 'address')}));
        qs.push(helper.generateQuestion('cmd', 'input', 'Command (msgh): ', {default: answerCache.get('query', 'cmd')}));
        qs.push(helper.generateQuestion('file','list','Select payload file: ',{choices: getPayloadListing()}));

        self.prompt(qs, function(res) {
            let json = helper.readJsonFile(payloadDir() + '/' + res.file);
            answerCache.set('query', 'address', res.address);
            answerCache.set('query', 'cmd', res.cmd);
            answerCache.save();
            let loggedIn = () => {
                rest.query(res.address, res.cmd, json)
                    .then(data => {
                        console.log(JSON.stringify(data));
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });
    });