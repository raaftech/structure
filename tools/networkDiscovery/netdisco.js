"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const nmapLib = require('./nmapLib');
const Helper = require('./helper');
const RestConnector = require('./restConnector');
const graphLib = require('./graphLib');

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

const main = (db) => {
    let args = R.defaultTo('-p22,80', options.args);
    let cidr = R.defaultTo('localhost', options.cidr);
    const helper = new Helper(db); 
    rest.setToken(options.token);
    let network;
    let reuseId = '';

    rest.getInstanceByPredicate('cidr', cidr)
        .then(cidrInstance => {
            if(R.isEmpty(cidrInstance)) {
                console.log('CREATING A NEW NETWORK BASED ON: '+cidr);
                network = helper.createNetwork(cidr, options.net);
            } else {
                console.log('REUSING NETWORK BASED ON: '+cidr);
                network = cidrInstance;
                reuseId = network.meta.uuid;
            }

            nmapLib.nmapScan(cidr, args)
                .then((nmapResults) => {
                    let graph = helper.transform(network, nmapResults);
                    if(!R.isEmpty(reuseId)) {
                        graph = R.tail(graph);
                    }
                    let promises = [];
                    R.forEach(item => {
                        promises.push(rest.createInstance({entities: [item]}));
                    }, graph);
                    log(helper.getTypeMap(graph));
                    return Promise.all(promises);
                })
                .then(result => {
                    console.log(result);
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