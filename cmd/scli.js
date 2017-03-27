"use strict";
const vorpal = require('vorpal')();
const R = require('ramda');
const util = require('util');
const helper = require('./helper');
const clc = require('cli-color');
const sprintf = require('sprintf-js').sprintf;
const treeify = require('treeify');
const neo = require('./extensions/neo');
const fs = require('fs');
const path = require('path');
const RestConnector = require('./restConnector');
const EventSource = require('eventsource');

const notNil = R.compose(R.not, R.isNil);

let rest;

init();

function init() {
    rest = new RestConnector(
        R.defaultTo('localhost', process.env.RESTHOST),
        R.defaultTo(3000, process.env.RESTPORT)
    );
    setupStatusChannel();
    scli();
}

function scli() {
	vorpal.history('structure-history');
	vorpal.use(neo);
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

//TODO remove this after the restful variant of this function is fully implemented
function DEPRECATEDsetupStatusChannel() {
    // eb.registerHandler('com.raaftech.structure.status',function(error, message) {
    //     if(error) {
    //         console.log("ERROR: "+error);
    //     } else {
    //         let uuid = message.body.data.uuid;
    //         if(!R.isNil(uuid)) {
    //             sendEvent(helper.getEventBusAddress(['repository','types']),'findOne',{node: uuid})
    //                 .then((data) => {
    //                     console.log(helper.stringify(data));
    //                     let comps = helper.getResultData(data)["components"];
    //                     comps = helper.consolidateComponents(clc.green(uuid), comps);
    //                     let name = helper.getResultData(data)['name'];
    //                     if(name.indexOf('unknown-') !== -1) name = clc.red(name);
    //                     else name = clc.green(name);
    //                     let tree = {};
    //                     tree[name] = comps;
    //                     console.log(treeify.asTree(tree, true));
    //                 });
    //         }
    //     }
    // });
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
                    let getIsaParent = R.pipe(R.path(['meta','relations','ISA','uuids']), R.head);
                    resolve(toRoot(getIsaParent(parent), tree));
                }
            })
    });
}

function payloadDir() {
    return path.join(__dirname, 'payloads');
}

function getPayloadListing() {
    return fs.readdirSync(payloadDir());
}

vorpal.command('login', 'Get an access token for Structure')
    .action(function(args, done) {
        const self = this;
        let qs = [];
        qs.push(helper.generateQuestion('email', 'input', 'Email: '));
        qs.push(helper.generateQuestion('password', 'password', 'Password: '));
        self.prompt(qs, function(answers) {
            rest.login(answers.email, answers.password)
                .then(user => {
                    let handler = R.ifElse(
                        R.has('error'),
                        error => self.log(util.format('%s (%s): %s',clc.red.bold(error.error.code), clc.red(error.error.statusCode), error.error.message)),
                        user => self.log(clc.green('AccessToken: ')+user.id)
                    );
                    handler(user);
                    done();
                })
                .catch(ex => {
                    self.log(ex);
                    done();
                });
        });
    });

vorpal.command('logout', 'Invalidate an access token for Structure')
    .action(function(args, done) {
        const self = this;
        self.prompt(qs, function(answers) {
            rest.logout()
                .then(result => {
                    self.log("Logged out");
                    done();
                })
                .catch(ex => {
                    self.log(ex);
                    done();
                });
        });
    });

vorpal.command('adduser', 'Add a structure user')
    .action(function(args, done) {
        const self = this;
        let qs = [];
        qs.push(helper.generateQuestion('email', 'input', 'Email: '));
        qs.push(helper.generateQuestion('password', 'password', 'Password: '));
        qs.push(helper.generateQuestion('confirm', 'password', 'Re-enter password: '));
        self.prompt(qs, function(answers) {
            let createUserHandler = R.ifElse(
                R.has('error'),
                error => self.log(util.format('status code: %s\n%s', clc.red.bold(error.error.statusCode), error.error.message)),
                user => self.log(util.format('%s "%s" (userid: %s) %s', clc.green('User'), user.email, user.id, clc.green('created')))
            );
            let createUser = () => rest.createUser(answers.email, answers.password)
                .then(result => {
                    createUserHandler(result);
                    done();
                })
                .catch(ex => {
                    self.log(ex);
                    done();
                });
            let pwdCheck = R.ifElse(R.equals, createUser, () => {self.log(clc.red('Passwords to not match')); done();});
            pwdCheck(answers.password, answers.confirm);
        });
    });

//todo extract to extension
vorpal.command('structure', 'Structure admin tools')
    .option('-s, --status', 'Show structure status information')
    .option('-i, --init', 'Purge-Init structure server')
    .action(function(args, done) {
        const self = this;
        let handler = (method) => {
            rest[method]()
                .then(result => {
                    self.log(clc.green.bold('[JVM info]'));
                    self.log(util.format('  Version: %s', result.jvm.version));
                    self.log(util.format('  Java version: %s',result.jvm.java_version));
                    self.log(util.format('  Cpus: %s', result.jvm.available_processors));
                    self.log(util.format(clc.green('  [Memory]')));
                    self.log(util.format('    Used: %s\n    Free: %s',result.jvm.memory.used, result.jvm.memory.free));

                    self.log(util.format(clc.green.bold('[Graph info]')));
                    self.log(util.format('  Types: %s', result.graph.types));
                    self.log(util.format('  Components: %s', result.graph.components));
                    self.log(util.format('  Instruments: %s', result.graph.instruments));
                    self.log(util.format('  Groupings: %s', result.graph.groupings));
                    self.log(util.format('  Results: %s', result.graph.results));
                    self.log(util.format('  Configurations: %s', result.graph.configurations));
                    done();
                })
                .catch(ex => {self.log(ex); done();})
        }

        let pathNotNil = R.curry((prop, obj) => notNil(R.path(['options', prop], obj)));
        let ex = R.cond([
            [pathNotNil('status'), () => handler('getStructureStatusInfo')],
            [pathNotNil('init'), () => handler('purgeInit')],
            [R.T, done()]
        ]);
        ex(args);
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
                    let meta = R.curry((ent,list) => {
                        return R.pipe(R.prop('meta'), R.keys,
                            R.map(item => 'meta.'+item),
                            R.concat(list)
                        )(ent);
                    });
                    let keys = R.pipe(R.keys, R.prepend('ALL'),meta(entity))(entity);
                    self.prompt(helper.generateQuestion('list', 'list', 'Choose a field', {choices: keys}), function(result) {
                        let check = R.ifElse(R.propEq('list','ALL'),
                                        () => self.log(helper.stringify(entity)),
                                        () => {
                                            let pp = R.split('.',result.list);
                                            let prop = clc.green.bold(result.list);
                                            self.log(util.format('%s: %s',prop,helper.stringify(R.path(pp, entity))));
                                        }
                                    );
                        check(result);
                        done();
                    });
                })
                .catch(ex => {
                    self.log(ex);
                    done();
                });
        };

        let exec = R.ifElse(R.isNil,
            () => handler('getEntityByName', args.name),
            () => handler('getEntityByUuid', args.name));
        exec(R.path(['options', 'uuid'], args));
    });

vorpal.command('instance')
    .option('-l, --last <quantity>', 'Fetch the last (quantity) created instances')
    .option('-f, --find <name>', 'Find a specific instance by name')
    .action(function(args, done) {
        const self = this;
        let promise = args.options.last ? rest.lastInstances(args.options.last) : rest.getInstanceByUuid(args.options.find);

        promise.then(instance => {
            self.log(helper.stringify(instance));
            done();
        }).catch(ex => {
                self.log(ex);
                done();
            });
    });

vorpal.command('dig <uuid>', 'Get the child elements for a node')
    .action(function(args, done) {
        const self = this;

        getChildren(args.uuid)
            .then(children => {
                R.forEach((item) => {
                    self.log(sprintf('%-16s: %24s', R.prop('name', item),R.prop('uuid',item)));
                }, children);
                done();
            })
            .catch(ex => {
                self.log(ex);
                done();
            })
    });

vorpal.command('tree <name>', 'Show a relation tree for the requested node')
    .option('-u, --uuid', 'Make the search go by uuid instead of name')
    .action(function(args, done) {
        const self = this;
        let rootUuid = '';
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
                .catch(ex => {
                    self.log(ex);
                    done();
                });
        };

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

        let loggedIn = () => rest.createInstance(pargs)
            .then(created => {
                let instanceUuid = created.meta.uuid;
                return rest.runInstrument('IpAddressDiscover', instanceUuid);
            })
            .then(result => done())
            .catch(ex => {
                self.log(ex);
                done();
            });
        let loggedOut = () => { console.log('Please login first.'); done()};
        let handler = R.ifElse(R.isNil,loggedOut, loggedIn);
        handler(rest.getToken());
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

            rest.query(res.address, res.cmd, json)
                .then(data => {
                    console.log(data);
                    done();
                })
                .catch(ex => {
                    console.log(ex);
                    done();
                });
        });
    });