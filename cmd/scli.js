"use strict";
const vorpal = require('vorpal')();
const R = require('ramda');
const util = require('util');
const helper = require('./helper');
const clc = require('cli-color');
const sprintf = require('sprintf-js').sprintf;
const treeify = require('./treeify');
const neo = require('./extensions/neo');
const structure = require('./extensions/structure');
const fs = require('fs');
const path = require('path');
const RestConnector = require('./restConnector');
const EventSource = require('eventsource');
const Structure = require('./subgraph');

const notNil = R.compose(R.not, R.isNil);
const genericAuthCommandHandler = R.curry((done, loggedInFn, token) => {
    let loggedOut = () => { console.log('Please login first.'); done()};
    let handler = R.ifElse(R.isNil,loggedOut, loggedInFn);
    handler(token);
});
const genericCatchHandler = R.curry((done, error) => {
    console.log(error);
    done();
});

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
	vorpal.use(structure, rest);
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
                    let keys = R.pipe(R.keys, R.prepend('ALL'),meta(entity))(entity);
                    self.prompt(helper.generateQuestion('list', 'list', 'Choose a field', {choices: keys}), function(result) {
                        let check = R.ifElse(R.propEq('list','ALL'),
                                        () => self.log(helper.treePrint(entity)),
                                        () => {
                                            let pp = R.split('.',result.list);
                                            let prop = clc.green.bold(result.list);
                                            let outputTypeSelector = R.ifElse(R.is(Object),
                                                x => self.log(util.format('%s\n%s', prop, helper.treePrint(R.path(pp, entity)))),
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

vorpal.command('instance')
    .option('-l, --last <quantity>', 'Fetch the last (quantity) created instances')
    .option('-f, --find <name>', 'Find a specific instance by name')
    .action(function(args, done) {
        const self = this;
        let promise = args.options.last ? rest.lastInstances(args.options.last) : rest.getInstanceByUuid(args.options.find);

        promise.then(instance => {
            R.pipe(R.ifElse(R.is(Object), helper.treePrint, R.identity),
                   x => console.log(x),
                   x => done()) (instance);
        }).catch(genericCatchHandler(done));
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
            .catch(genericCatchHandler(done))
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

vorpal.command('grouping <name>', 'Create a new Grouping')
    .action(function(args, done) {
        let fetchGrouping = () => {
            rest.getEntityByUuid('5f1eb7c5-ec5e-4563-be6a-fc50774fc374')
                .then(entity => {
                    let groupingType = new Structure.SubGraph(entity);

                    entity = R.assoc('type', 'Instance', entity);
                    entity = R.assoc('name', args.name, entity);
                    entity = R.dissocPath(['meta', 'uuid'], entity);
                    entity = R.dissocPath(['meta', 'checksum'], entity);

                    let subgraph = new Structure.SubGraph([]);

                    return entity;
                })
                .then(payload => rest.createInstance({entities: [payload]}))
                .then(created => {
                    console.log(created);
                    done();
                })
        };

        let loggedIn = () => rest.createInstance(payload)
            .then(created => {
                console.log(created);
                done();
            })
            .catch(genericCatchHandler(done));
        genericAuthCommandHandler(done, fetchGrouping, rest.getToken());
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
                .catch(genericCatchHandler(done));
        });
    });