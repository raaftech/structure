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

const genericAuthCommandHandler = R.curry((done, loggedInFn, token) => {
    let loggedOut = () => { console.log('Please login first.'); done()};
    let handler = R.ifElse(R.isNil,loggedOut, loggedInFn);
    handler(token);
});
const genericCatchHandler = R.curry((done, error) => {
    console.log(error);
    done();
});
const notNil = R.compose(R.not, R.isNil);
const entity2Graph = (entity) => {
    let container = {};
    container[entity.meta.uuid] = entity;
    let graph = new Structure.SubGraph(container);
    return graph;
};
const createInstance = (baseTypeEntity, instanceEntity) => {
    let rel = instanceEntity.relationCreate('INSTANCE_OF', 'outgoing');
    rel.addEntity(baseTypeEntity);
    let container = {};
    container[instanceEntity.meta.uuid] = instanceEntity;
    return container;
};

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
                .catch(genericCatchHandler(done));
        };

        //Function that determines which rest call to use based on the given arguments and options
        let exec = R.ifElse(R.isNil,
            () => handler('getEntityByName', args.name),
            () => handler('getEntityByUuid', args.name));
        exec(R.path(['options', 'uuid'], args));
    });

vorpal.command('instanceof <typeName>', 'Create an instance of a Type')
    .action(function(args, done) {
        const self = this;
        let baseType;
        let buffer;

        rest.getEntityByName(args.typeName)
            .then(entity => {
                baseType = helper.json2Entity(false, entity);
                buffer = entity;
                return getDomainClass(baseType.meta.uuid);
            })
            .then(domainClass => {
                console.log('Resolved domain class: '+domainClass);
                buffer = R.pipe(
                    R.assoc('type', domainClass),
                    R.assoc('name', buffer.name),
                    R.dissocPath(['meta','checksum']),
                    R.dissocPath(['meta','relations'])) (buffer);
                return buffer;
            })
            .then(instance => {
                validator.getSchema(args.typeName)
                    .then(typeSchema => {
                        let getPropertyKeys = R.pipe(R.prop('properties'), R.keys);
                        let createQuestions = (schema) => {
                            let keys = getPropertyKeys(schema);
                            return R.map(prop => {
                                let property = R.path(['properties', prop], schema);
                                let pre = util.format('[%s]\n',clc.green(prop));
                                if(notNil(property.type)) pre += util.format('\ttype: %s\n', clc.red(property.type));
                                if(notNil(property.description)) pre += util.format('\tdescription: %s\n', property.description);
                                pre += 'Value: '

                                let valueFetcher = R.cond([
                                    [R.equals('name'), x => R.prop('name', instance)],
                                    [R.equals('desc'), x => R.prop('desc', instance)],
                                    [R.equals('checksum'), x => R.path(['meta','checksum'], instance)],
                                    [R.T, x => R.path(['data', x], instance)]
                                ]);

                                return helper.generateQuestion(prop, 'input', pre, {default: valueFetcher(prop)});
                            }, keys);
                        };

                        //remove if: (prop not in required list) AND (input-value for prop is null|undefined|empty)
                        let sanitizeAnswer = (required, answer) => {
                            let output = {};
                            R.forEach(key => {
                                if(R.contains(key, required)) {
                                    output = R.assoc(key, answer[key], output);
                                } else {
                                    if(!R.or(R.isNil(answer[key]), R.isEmpty(R.trim(answer[key])))) {
                                        output = R.assoc(key, answer[key], output);
                                    } else {
                                        console.log('Ignoring optional, empty property: %s', clc.green(key));
                                    }
                                }
                            }, R.keys(answer));
                            return output;
                        };

                        self.prompt(createQuestions(typeSchema), function(res) {
                            res = sanitizeAnswer(typeSchema.required, res);
                            if(R.isNil(instance.data)) instance.data = res;
                            else instance.data = R.merge(instance.data,res);

                            validator.validate(args.typeName, instance.data)
                                .then(vres => {
                                    if(vres.valid) {
                                        instance.data = R.merge(instance.data, vres.instance);
                                        //TODO validate if this is feasible
                                        instance.name = instance.data.name;
                                        instance.desc = instance.data.desc;
                                        
                                        instance = createInstance(baseType, helper.json2Entity(true, instance));
                                        let graph = new Structure.SubGraph(instance).toList();
                                        console.log(JSON.stringify(graph, undefined, 2));
                                        let loggedIn = () => {
                                            console.log('creating instance');
                                            rest.createInstance({entities: graph})
                                                .then(updated => {
                                                    if(R.isEmpty(updated)) console.log('No changes were made');
                                                    else console.log(updated);
                                                    done();
                                                })
                                                .catch(ex => {console.log(ex); done();})
                                        };
                                        genericAuthCommandHandler(done, loggedIn, rest.getToken());
                                    } else {
                                        console.log(vres);
                                        done();
                                    }
                                });
                        });
                    });
            })
            .catch(genericCatchHandler(done));
    })

vorpal.command('edit <uuid>', 'Edit the properties of an instance')
    .action(function(args,done) {
        const self = this;

        console.log(clc.green('(Input a space if you wish to override default values)'));
        rest.getInstanceByUuid(args.uuid)
            .then(instance => {
                validator.getSchema(instance.type)
                    .then(typeSchema => {
                        let getPropertyKeys = R.pipe(R.prop('properties'), R.keys);
                        let createQuestions = (schema) => {
                            let keys = getPropertyKeys(schema);
                            return R.map(prop => {
                                let property = R.path(['properties', prop], schema);
                                let pre = util.format('[%s]\n',clc.green(prop));
                                if(notNil(property.type)) pre += util.format('\ttype: %s\n', clc.red(property.type));
                                if(notNil(property.description)) pre += util.format('\tdescription: %s\n', property.description);
                                pre += 'Value: '

                                let valueFetcher = R.cond([
                                    [R.equals('name'), x => R.prop('name', instance)],
                                    [R.equals('desc'), x => R.prop('desc', instance)],
                                    [R.equals('checksum'), x => R.path(['meta','checksum'], instance)],
                                    [R.T, x => R.path(['data', x], instance)]
                                ]);

                                return helper.generateQuestion(prop, 'input', pre, {default: valueFetcher(prop)});
                            }, keys);
                        };

                        //remove if: (prop not in required list) AND (input-value for prop is null|undefined|empty)
                        let sanitizeAnswer = (required, answer) => {
                            let output = {};
                            R.forEach(key => {
                                if(R.contains(key, required)) {
                                    output = R.assoc(key, answer[key], output);
                                } else {
                                    if(!R.or(R.isNil(answer[key]), R.isEmpty(R.trim(answer[key])))) {
                                        output = R.assoc(key, answer[key], output);
                                    } else {
                                        console.log('Ignoring optional, empty property: %s', clc.green(key));
                                    }
                                }
                            }, R.keys(answer));
                            return output;
                        };

                        self.prompt(createQuestions(typeSchema), function(res) {
                            res = sanitizeAnswer(typeSchema.required, res);
                            if(R.isNil(instance.data)) instance.data = res;
                            else instance.data = R.merge(instance.data,res);
                            validator.validate(instance.type, instance.data)
                                .then(vres => {
                                    if(vres.valid) {
                                        instance.data = R.merge(instance.data, vres.instance);
                                        let loggedIn = () => {
                                            rest.updateInstance({entities: [instance]})
                                                .then(updated => {
                                                    if(R.isEmpty(updated)) console.log('No changes were made');
                                                    else console.log(updated);
                                                    done();
                                                })
                                                .catch(ex => {console.log(ex); done();})
                                        };
                                        genericAuthCommandHandler(done, loggedIn, rest.getToken());
                                    } else {
                                        console.log(vres);
                                        done();
                                    }
                                });
                        });
                    });
            })
            .catch(genericCatchHandler(done));
    });

vorpal.command('instance')
    .option('-l, --last <quantity>', 'Fetch the last (quantity) created instances')
    .option('-u, --uuid <uuid>', 'Find a specific instance by uuid')
    .option('-n, --name <name>', 'Find a specific instance by name')
    .option('-r, --remove <uuid>', 'Remove a specific instance')
    .option('-v, --verbose', 'Be verbose in printing the results')
    .action(function(args, done) {
        //Function that checks if a property is not nil in the given object.options object
        let fn = (prop, obj) => R.pipe(R.path(['options', prop]), notNil);
        let predicate = R.cond([
            [fn('last'), (x) => rest.lastInstances(R.path(['options','last'], x))],
            [fn('uuid'), (x) => rest.getInstanceByUuid(R.path(['options','uuid'], x))],
            [fn('name'), (x) => rest.getInstanceByName(R.path(['options','name'], x))],
            [fn('remove'), (x) => rest.removeInstance(R.path(['options','remove'], x))],
            [R.T, () => Promise.resolve([])]
        ]);
        let promise = predicate(args);

        promise.then(instance => {
            R.pipe(R.ifElse(R.is(Object), helper.treePrint(args.options.verbose), R.identity),
               x => console.log(x),
               x => done()) (instance);
        }).catch(genericCatchHandler(done));
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
                .catch(genericCatchHandler(done));
        } else {
            getChildren(args.options.uuid)
                .then(children => {
                    R.forEach((item) => {
                        self.log(sprintf('%-16s: %24s', R.prop('name', item),R.prop('uuid',item)));
                    }, children);
                    done();
                })
                .catch(genericCatchHandler(done))
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
                .catch(genericCatchHandler(done));
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
            .catch(genericCatchHandler(done));

        genericAuthCommandHandler(done, loggedIn, rest.getToken());
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
                    .catch(genericCatchHandler(done));
            };

            genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });
    });