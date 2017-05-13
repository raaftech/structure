'use strict';

const vorpal = require('vorpal')();
const neo4j = require('neo4j');
const R = require('ramda');
const util = require('util');
const path = require('path');
const clc = require('cli-color');
const helper = require(path.join(__dirname, '..','helper'));
const Structure = require(path.join(__dirname, '..', 'subgraph'));

module.exports = function(vorpal, options) {
    const toRoot = (uuid, childNodes) => {
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
     };
    const getDomainClass = (uuid) => {
         return new Promise((resolve, reject) => {
             toRoot(uuid, {})
                 .then(parents => {
                     let fn = R.pipe(R.prop('Type'),R.keys,R.head);
                     resolve(fn(parents));
                 })
                 .catch(ex => reject(ex));
         });
     };

    const rest = options.rest;
    const validator = options.validator;

    vorpal.command('instance')
        .option('-l, --last <quantity>', 'Fetch the last (quantity) created instances')
        .option('-u, --uuid <uuid>', 'Find a specific instance by uuid')
        .option('-n, --name <name>', 'Find a specific instance by name')
        .option('-r, --remove <uuid>', 'Remove a specific instance')
        .option('-v, --verbose', 'Be verbose in printing the results')
        .action(function(args, done) {
            const self = this;
            //Function that checks if a property is not nil in the given object.options object
            let fn = (prop, obj) => R.pipe(R.path(['options', prop]), helper.notNil);
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
                   x => self.log(x),
                   x => done()) (instance);
            }).catch(helper.genericCatchHandler(done));
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
                    self.log('Resolved domain class: '+domainClass);
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
                                    if(helper.notNil(property.type)) pre += util.format('\ttype: %s\n', clc.red(property.type));
                                    if(helper.notNil(property.description)) pre += util.format('\tdescription: %s\n', property.description);
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
                                            self.log('Ignoring optional, empty property: %s', clc.green(key));
                                        }
                                    }
                                }, R.keys(answer));
                                return output;
                            };

                            self.log(clc.green('(Input a space if you wish to override default values)'));
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

                                            instance = helper.createInstance(baseType, helper.json2Entity(true, instance));
                                            let graph = new Structure.SubGraph(instance).toList();
                                            self.log(JSON.stringify(graph, undefined, 2));
                                            let loggedIn = () => {
                                                self.log('creating instance');
                                                rest.createInstance({entities: graph})
                                                    .then(updated => {
                                                        if(R.isEmpty(updated)) self.log('No changes were made');
                                                        else self.log(updated);
                                                        done();
                                                    })
                                                    .catch(ex => {self.log(ex); done();})
                                            };
                                            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
                                        } else {
                                            self.log(vres);
                                            done();
                                        }
                                    });
                            });
                        });
                })
                .catch(helper.genericCatchHandler(done));
        })

    vorpal.command('edit <uuid>', 'Edit the properties of an instance')
        .action(function(args,done) {
            const self = this;
            let currentInstance;

            let getInstanceOf = R.compose(R.head, R.path(['meta','relations','INSTANCE_OF','uuids']));

            self.log(clc.green('(Input a space if you wish to override default values)'));
            rest.getInstanceByUuid(args.uuid)
                .then(instance => {
                    currentInstance = instance;
                    console.log(getInstanceOf(instance));
                    return rest.getEntityByUuid(getInstanceOf(instance));
                })
                .then(entity => {
                    validator.getSchema(entity.name)
                        .then(typeSchema => {
                            //console.log(JSON.stringify(typeSchema, undefined, 2));
                            let getPropertyKeys = R.pipe(R.prop('properties'), R.keys);
                            let createQuestions = (schema) => {
                                let keys = getPropertyKeys(schema);
                                return R.map(prop => {
                                    let property = R.path(['properties', prop], schema);
                                    let pre = util.format('[%s]\n',clc.green(prop));
                                    if(helper.notNil(property.type)) pre += util.format('\ttype: %s\n', clc.red(property.type));
                                    if(helper.notNil(property.description)) pre += util.format('\tdescription: %s\n', property.description);
                                    pre += 'Value: '

                                    let valueFetcher = R.cond([
                                        [R.equals('name'), x => R.prop('name', currentInstance)],
                                        [R.equals('desc'), x => R.prop('desc', currentInstance)],
                                        [R.equals('checksum'), x => R.path(['meta','checksum'], currentInstance)],
                                        [R.T, x => R.path(['data', x], currentInstance)]
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
                                            self.log('Ignoring optional, empty property: %s', clc.green(key));
                                        }
                                    }
                                }, R.keys(answer));
                                return output;
                            };

                            self.prompt(createQuestions(typeSchema), function(res) {
                                res = sanitizeAnswer(typeSchema.required, res);
                                if(R.isNil(currentInstance.data)) currentInstance.data = res;
                                else currentInstance.data = R.merge(currentInstance.data,res);
                                validator.validate(entity.name, currentInstance.data)
                                    .then(vres => {
                                        if(vres.valid) {
                                            currentInstance.data = R.merge(currentInstance.data, vres.instance);
                                            currentInstance.name = R.defaultTo(currentInstance.name, currentInstance.data.name);
                                            currentInstance.desc = R.defaultTo(currentInstance.desc, currentInstance.data.desc);
                                            let loggedIn = () => {
                                                rest.updateInstance({entities: [currentInstance]})
                                                    .then(updated => {
                                                        if(R.isEmpty(updated)) self.log('No changes were made');
                                                        else self.log(updated);
                                                        done();
                                                    })
                                                    .catch(ex => {self.log(ex); done();})
                                            };
                                            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
                                        } else {
                                            self.log(vres);
                                            done();
                                        }
                                    });
                            });
                        });
                })
                .catch(helper.genericCatchHandler(done));
        });


    vorpal.command('run instrument <uuid>', 'Run an instrument against an instance')
        .action(function(args, done) {
            const self = this;
            let availableInstruments;
            let getUserSupplied = (obj) => {
                obj = JSON.parse(obj);
                let result = {};
                R.forEachObjIndexed((val, key) => {
                    if(val.value.from === 'static' && val.canBeUserSupplied) {
                        result[key] = val;
                    }
                }, obj);
                return result;
            };
            let createQuestions = (obj) => {
                let keys = R.keys(obj);
                return R.map(item => {
                    let q = util.format('%s ::%s [%s]: ', item, obj[item].value.type, obj[item].isOptional ? 'optional' : 'required');
                    return helper.generateQuestion(item, 'input', q);
                }, keys);
            };

            let loggedIn = () => {
                rest.getInstruments(args.uuid)
                    .then(instruments => {
                        self.log(instruments);
                        availableInstruments = instruments;
                        let q = helper.generateQuestion('instrument', 'list', 'Select instrument: ', {choices: R.keys(instruments)});
                        self.prompt(q, function(result) {
                            rest.getInstanceByUuid(availableInstruments[result.instrument])
                                .then(instance => {
                                    let argObj = getUserSupplied(instance.data.arguments);
                                    let qs = createQuestions(argObj);
                                    self.prompt(qs, function(res) {
                                        rest.runInstrument(args.uuid, result.instrument, res)
                                            .then(instResult => {
                                                self.log(instResult);
                                                done();
                                            });
                                    });
                                })
                        });
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });


    vorpal.command('run capability <uuid>', 'Run a capability for an instance')
        .action(function(args, done) {
            const self = this;
            const capabilityList = [
                'Show','State', 'List','Identify','Detect', 'Verify', 'Create', 'Destroy','Execute', 'Guess'
            ];

            let createQuestions = (obj) => {
                let keys = R.keys(obj);
                return R.map(item => {
                    let q = util.format('%s ::%s [%s]: ', item, obj[item].value.type, obj[item].isOptional ? 'optional' : 'required');
                    return helper.generateQuestion(item, 'input', q);
                }, keys);
            };

            let loggedIn = () => {
                let q = helper.generateQuestion('capability', 'list', 'Select capability to run: ', {choices: capabilityList});
                self.prompt(q, function(answer) {
                    rest.runCapability(args.uuid, answer.capability)
                        .then(result => {
                            console.log(result);
                            done();
                        });
                });
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });
};