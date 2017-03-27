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

//TODO fetch these params from env or config
let rest = new RestConnector('localhost', 3000);

init();

function init() {
    setupStatusChannel();
    scli();
}

function scli() {
	vorpal.history('structure-history');
	vorpal.use(neo);
	vorpal.delimiter(clc.magenta.bold('structure$')).show();
}

//TODO fetch params from env or config
function setupStatusChannel() {
    const changeUrl = 'http://localhost:3000/api/Statuses/change-stream';
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
        qs.push(helper.generateQuestion('email', 'input', 'Username: '));
        qs.push(helper.generateQuestion('password', 'password', 'Password: '));
        self.prompt(qs, function(answers) {
            rest.login(answers.email, answers.password)
                .then(user => {
                    if(R.has('error', user)) {
                        self.log(util.format('%s (%s): %s',user.error.code,user.error.statusCode, user.error.message));
                    } else {
                        self.log('AccessToken: '+user.id);
                    }
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

        if(args.options.uuid) {
            handler('getEntityByUuid', args.name);
        } else {
            handler('getEntityByName', args.name);
        }
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

        rest.createInstance(pargs)
            .then(created => {
                let instanceUuid = created.meta.uuid;
                return rest.runInstrument('IpAddressDiscover', instanceUuid);
            })
            .then(result => done())
            .catch(ex => {
                self.log(ex);
                done();
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