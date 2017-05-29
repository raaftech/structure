"use strict";
const vorpal = require('vorpal')();
const R = require('ramda');
const util = require('util');
const helper = require('./helper');
const clc = require('cli-color');
const treeify = require('./treeify');
const fs = require('fs');
const path = require('path');
const RestConnector = require('./restConnector');
const SchemaValidator = require('./SchemaValidator');
const SimpleCache = require('./simpleCache');
const EventSource = require('eventsource');
const graphLib = require('./graphLib');
//extensions
const neoExt = require('./extensions/neo');
const structureExt = require('./extensions/structure');
const groupExt = require('./extensions/groups');
const instanceExt = require('./extensions/instance');


const answerCache = new SimpleCache();
const memoryBank = {a: '', b: '',token: ''};
let typeDb = [];
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
    if(answerCache.get('structure','token') !== '') {
        rest.setToken(answerCache.get('structure','token'));
        memoryBank.token = answerCache.get('structure', 'token');
    }

    setupStatusChannel();
    rest.getAllTypes()
        .then(allTypes => {
            typeDb = allTypes;
            scli();
        })
        .catch(ex => console.log(ex));
}

function scli() {
	vorpal.history('structure-history');
	vorpal.use(neoExt, answerCache);
	vorpal.use(structureExt, {rest: rest, memoryBank: memoryBank, cache: answerCache});
	vorpal.use(groupExt, rest);
	vorpal.use(instanceExt, {rest: rest, validator: validator, typeDb: typeDb});
	vorpal.delimiter(clc.magenta.bold('structure$')).show();

	vorpal.on('keypress', function(key) {
	    if(key.e.key.ctrl) {
	        if(key.key === 'a') {
	            vorpal.ui.input(vorpal.ui.input() + memoryBank.a);
	        } else if(key.key === 'b') {
	            vorpal.ui.input(vorpal.ui.input() + memoryBank.b);
	        } else if(key.key === 't') {
	            vorpal.ui.input(vorpal.ui.input() + memoryBank.token);
	        }
	    }
	});
}

function setupStatusChannel() {
    const changeUrl = rest.getChangeStreamUrl();
    const es = new EventSource(changeUrl);
    es.addEventListener('data', function(message) {
        let data = JSON.parse(message.data);
        let output = util.format('Type: %s\nEvent: %s\nData: %s',
                                 clc.green(R.path(['type'],data)),
                                 clc.green(R.path(['data','event'],data)),
                                 clc.green(JSON.stringify(R.path(['data','data'],data),undefined,2)));
        vorpal.log(output);
    });
}

function payloadDir() {
    return path.join(__dirname, 'payloads');
}

function getPayloadListing() {
    return fs.readdirSync(payloadDir());
}

const prompt = (vorp, question) => {
    return new Promise((resolve, reject) => {
        vorp.prompt(question, function(result) {
            resolve(result);
        });
    });
};

vorpal.command('mem <bank> <data>', 'Save data to memory bank a or b. bank t automatically holds your token after login')
    .action(function(args, done) {
        const self = this;
        if(args.bank !== 'a' && args.bank !== 'b') {
            self.log('The value for <bank> needs to be "a" or "b"');
            done();
        } else {
            memoryBank[args.bank] = args.data;
            self.log('Data saved to memory bank [%s]. Print it with ctrl-%s', args.bank, args.bank);
            done();
        }
    });

vorpal.command('relate <fromInstanceUuid> <toInstanceUuid>', 'Create a new relation between two instances')
        .action(function(args, done) {
            const self = this;
            let fromInstance,toInstance;
            let requestedRelation;
            let currentConstraints;

            let instanceOfType = R.compose(R.head, R.path(['meta', 'relations', 'INSTANCE_OF', 'uuids']));
            let firstKey = R.compose(R.head, R.keys);
            let listRelations = R.compose(R.uniq, R.map(firstKey));
            let harvestEndpointUuids = (relName, list) => {
                return R.pipe(
                    R.filter(R.has(relName)),
                    R.map(R.prop(relName)),
                    R.map(R.prop('to'))
                )(list);
            };
            const findType = graphLib.findTypeByUuid(typeDb);
            const getId = R.path(['meta','uuid']);

            let loggedIn = () => {
                rest.getInstanceByUuid(args.fromInstanceUuid)
                    .then(instance => {
                        fromInstance = instance;
                        return rest.getInstanceByUuid(args.toInstanceUuid);
                    })
                    .then(instance => {
                        toInstance = instance;
                        return instanceOfType(fromInstance);
                    })
                    .then(uuid => rest.getConstraints(uuid))
                    .then(constraints => {
                        if(constraints.outgoing.length === 0) {
                            throw new Error('no possible constraints found, no relate possible');
                        } else {
                            currentConstraints = constraints;
                            let rels = listRelations(constraints.outgoing);
                            let q = helper.generateQuestion('relation', 'list', 'Select relation: ', {choices: rels});
                            return prompt(self, q);
                        }
                    })
                    .then(answer => {
                        requestedRelation = answer.relation;
                        return harvestEndpointUuids(answer.relation, currentConstraints.outgoing);
                    })
                    .then(uuids => {
                        return R.map(findType, uuids);
                    })
                    .then(result => {
                        self.log('Allowed types: %s',R.map(R.prop('name'), result));
                        self.log('%s %s %s', getId(fromInstance), requestedRelation, getId(toInstance));
                        if(R.isNil(R.path(['meta','relations',requestedRelation], fromInstance))) {
                            fromInstance.meta.relations[requestedRelation] = {direction: 'outgoing', uuids: [getId(toInstance)]};
                        } else {
                            fromInstance.meta.relations[requestedRelation].uuids.push(getId(toInstance));
                        }
                        return rest.updateInstance({entities: [fromInstance]});
                    })
                    .then(updated => {
                        self.log(updated);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
                };
            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

vorpal.command('specialize <uuid>', 'Specialize an instance')
    .action(function(args, done) {
            const self = this;
            const getId = R.path(['meta', 'uuid']);
            const kids = graphLib.getChildren(typeDb);
            const findType = graphLib.findTypeByUuid(typeDb);

            let selectedInstance;

            let loggedIn = () => {
                rest.getInstanceByUuid(args.uuid)
                    .then(instance => {
                        selectedInstance = instance;
                        let type = findType(graphLib.getInstanceOfUuid(selectedInstance));
                        return kids(getId(type));
                    })
                    .then(children => {
                        if(children.length === 0) {
                            self.log('No possible specializations found.');
                            done();
                        } else {
                            let q = helper.generateQuestion('child', 'list', 'Select specialisation: ', {choices: children});
                            self.prompt(q, function(answer) {
                                let e = R.find(R.propEq('name', answer.child), children);
                                rest.specializeInstance(getId(selectedInstance), getId(e))
                                    .then(updated => {
                                        self.log(updated);
                                        done();
                                    });
                            });
                        }
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

vorpal.command('find <name>', 'Find a specified node in structure')
    .option('-u, --uuid', 'Set search mode to uuid based search.')
    .option('-v, --verbose', 'Be verbose with the result output')
    .action(function(args, done) {
        const self = this;
        const findByUuid = graphLib.findTypeByUuid(typeDb);
        const fuzzyFind = graphLib.findTypeByFuzzyNameMatch(typeDb);

        //Function that calls a specific rest method, depending on the value of the method argument
        let handler = (method, arg) => {
            let entity;
            if(method === 'getEntityByName') {
                entity = fuzzyFind(arg);
            } else {
                entity = findByUuid(arg);
            }

            if(R.isNil(entity)) {
                self.log('Unable to find entity type: '+arg);
                done();
            } else {
                let meta = R.curry((ent,list) => {
                    return R.pipe(R.prop('meta'), R.keys,
                        R.map(item => 'meta.'+item),
                        R.concat(list)
                    )(ent);
                });

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
            }
            
        };

        //Function that determines which rest call to use based on the given arguments and options
        let exec = R.ifElse(R.isNil,
            () => handler('getEntityByName', args.name),
            () => handler('getEntityByUuid', args.name));
        exec(R.path(['options', 'uuid'], args));
    });



vorpal.command('tree <name>', 'Show a relation tree for the requested node')
    .option('-u, --uuid', 'Make the search go by uuid instead of name')
    .action(function(args, done) {
        const self = this;
        const findByUuid = graphLib.findTypeByUuid(typeDb);
        const fuzzyFind = graphLib.findTypeByFuzzyNameMatch(typeDb);
        const kids = graphLib.getChildren(typeDb);
        const getId = R.path(['meta','uuid']);

        let rootUuid = '';
        //Function that calls a specific rest method, depending on the value of the method argument
        let handler = (method, arg) => {
            let entity;
            if(method === 'getEntityByName') {
                entity = fuzzyFind(arg);
                rootUuid = getId(entity);
            } else {
                entity = findByUuid(arg);
                rootUuid = arg;
            }

            let tree = {};
            R.forEach((item) => {
                tree[item.name] = getId(item);
            }, kids(rootUuid));

            let dummyInstance = graphLib.createInstance(entity, {});
            let hierarchy = graphLib.getTypeHierarchy(typeDb, dummyInstance);
            R.forEach(item => {
                let tt = {};
                tt[item.name] = R.clone(tree);
                tree = R.clone(tt);
            },R.reverse(hierarchy));
            
            self.log(treeify.asTree(tree,true));
            done();
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
        let byName = graphLib.findTypeByName(typeDb);
        let mkNetwork = graphLib.createInstance(byName('Ipv4Network'));
        let mkGroup = graphLib.createInstance(byName('ComponentGroup'));

        let loggedIn = () => rest.getInstanceByPredicate('cidr', args.cidr)
            .then(instance => {
                if(R.isEmpty(instance)) {
                    self.log('creating new network');
                    let i = mkNetwork({name: 'Ipv4Network', desc: 'A network', cidr: args.cidr});
                    let g = mkGroup({name: 'group-'+args.cidr, desc: 'ipv4network root group'});
                    let pair = graphLib.relateEntity(g, 'GROUPS', 'outgoing', i);
                    return rest.createInstance({entities: R.reverse(pair)})
                        .then(ins => rest.getInstanceByUuid(pair[1].meta.uuid))
                        .catch(ex => console.log(ex));
                } else {
                    self.log('reusing existing network');
                    return instance;
                }
            })
            .then(network => {
                return rest.runInstrument(network.meta.uuid, 'IpNetworkDiscover', {token: rest.getToken()});
            })
            .then(result => {
                self.log(result);
                self.log('done');
                done();
            })
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
                        self.log(JSON.stringify(data));
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });
    });