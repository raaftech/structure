"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const nmapLib = require('./nmapLib');
const Helper = require('./helper');
const RestConnector = require('./restConnector');

const rest = new RestConnector('localhost', 3000);
const log = (x) => console.log(JSON.stringify(x, undefined, 2));

const optionDefs = [
    {name: 'cidr', alias: 'c', type: String},
    {name: 'token', alias: 't', type: String},
    {name: 'net', alias: 'n', type: String},
    {name: 'args', alias: 'a', type: String}
];
const options = cmd(optionDefs);

const sections = [
    {
        header: 'structure bootstrap',
        content: 'Wraps the nmap command for discovering a network'
    },
    {
        header: 'Options',
        optionList: [
            {name: 'cidr', alias: 'c', typeLabel: '[underline]{cidr}', description: 'required argument, cidr to scan'},
            {name: 'token', alias: 't', typeLabel: '[underline]{token}', description: 'REST api token'},
            {name: 'net', alias: 'n', typeLabel: '[underline]{network name}]', description: 'Identifying network name in case of a network range'},
            {name: 'args', alias: 'a', typeLabel: '[underline]{nmap options}', description: 'arguments for the nmap command'}
        ]
    }
];
const usage = getUsage(sections);

const getErrors = (list) => {
    let regex = /[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}/;
    let match = R.match(regex);
    let errors = R.filter(R.is(String),list);
    let fn = R.compose(R.head, match);
    return R.map(fn, errors);
};

const main = (db) => {
    let args = R.defaultTo('-p22,80', options.args);
    let cidr = R.defaultTo('localhost', options.cidr);
    const helper = new Helper(db); 
    rest.setToken(options.token);
    let network;
    let reuseId = false;

    rest.getInstanceByPredicate('cidr', cidr)
        .then(cidrInstance => {
            if(R.isEmpty(cidrInstance)) {
                console.log('CREATING A NEW NETWORK BASED ON: '+cidr);
                network = helper.createNetwork(cidr, options.net);
            } else {
                console.log('REUSING EXISTING NETWORK BASED ON: '+cidr);
                network = cidrInstance;
                reuseId = true;
                log(network);
            }

            nmapLib.nmapScan(cidr, args)
                .then((nmapResults) => {
                    let graph = helper.transform(network, nmapResults);
                    network = R.last(graph);
                    graph = R.init(graph);
                    
                    let promises = [];
                    R.forEach(item => {
                        promises.push(rest.createInstance({entities: [item]}));
                    }, graph);
                    log(helper.getTypeMap(graph));
                    return Promise.all(promises);
                })
                .then(result => {
                    let output = getErrors(result);
                    log(network);
                    if(R.length(output) !== 0) {
                        console.log('There are failed creations, removing them from network relations');
                        let fn = R.flip(R.contains)(output);
                        network.meta.relations['HAS'].uuids = R.reject(fn,network.meta.relations['HAS'].uuids);
                        log(network);
                        if(!reuseId) {
                            console.log('creating new network');
                            rest.createInstance({entities: [network]})
                                .then(createdNetwork => console.log(createdNetwork));
                        } else {
                            console.log('updating existing network');
                            rest.updateInstance({entities: [network]})
                                .then(createdNetwork => console.log(createdNetwork));
                        }
                    } else {
                        console.log('no errors');
                    }
                });
        })
        .catch(ex => console.log(ex));
};

if(R.isNil(options.cidr)) {
    console.log('A cidr is required');
    console.log(usage);
    process.exit(1);
}

if(R.isNil(options.token)) {
    console.log('A REST api token is required');
    console.log(usage);
    process.exit(1);
}

rest.getAllTypes().then(types => main(types))
    .catch(ex => console.log('Failed to fetch all types from structure. (is your token valid?): %s', ex));