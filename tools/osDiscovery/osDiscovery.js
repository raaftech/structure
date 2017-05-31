"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const ansibleLib = require('./ansibleLib');
const Helper = require('./helper');
const RestConnector = require('./restConnector');

const rest = new RestConnector('localhost', 3000);
const log = (x) => console.log(JSON.stringify(x, undefined, 2));

const optionDefs = [
    {name: 'token', alias: 't', type: String},
    {name: 'ip', alias: 'i', type: String}
];
const options = cmd(optionDefs);

const sections = [
    {
        header: 'Ansible OS discovery',
        content: 'Wraps the ansible command for discovering os setup for an ipv4address'
    },
    {
        header: 'Options',
        optionList: [
            {name: 'token', alias: 't', typeLabel: '[underline]{token}', description: 'REST api token'},
            {name: 'ip', alias: 'i', typeLabel: '[underline]{ip}', description: 'The IP address to scan with ansible'}
        ]
    }
];
const usage = getUsage(sections);

const main = (db) => {
    const helper = new Helper(db);
    
    let ipEntity = '';
    let subGraph;
    rest.setToken(options.token);

    rest.getInstanceByPredicate('addr', options.ip)
        .then(ipAddress => {
            if(R.isEmpty(ipAddress)) {
                console.log('CREATING NEW Ipv4Address BASED ON: '+options.ip);
                ipEntity = helper.ipData(options.ip, true);
            } else {
                console.log('REUSING Ipv4Address BASED ON: '+options.ip);
                ipEntity = ipAddress;
            }

            ansibleLib.setup(options.ip)
                .then(ansibleResult => {
                    subGraph = helper.transform(ansibleResult, ipEntity);
                    log(subGraph);
                    let promises = [];
                    R.forEach(item => promises.push(rest.createInstance({entities: [item]})), subGraph.entities);
                    return Promise.all(promises);
                })
                .then(createdEntities => rest.createInstance({entities: [subGraph.os, subGraph.root]}))
                .then(finalResult => {
                    if(R.is(String, finalResult)) finalResult = JSON.parse(finalResult);
                    log(finalResult);
                    process.exit(0);
                });
        })
        .catch(ex => console.log(ex));
};

if(R.isNil(options.ip)) {
    console.log('An IP address is required');
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