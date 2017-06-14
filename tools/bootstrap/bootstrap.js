"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const ansibleLib = require('./ansibleLib');
const Helper = require('./helper');
const RestConnector = require('./restConnector');

const optionDefs = [
        {name: 'token', alias: 't', type: String},
        {name: 'compat', alias: 'c', type: Boolean},
        {name: 'hostname', alias: 'h', type: String}
    ];
const options = cmd(optionDefs);

const sections = [
    {
        header: 'structure bootstrap',
        content: 'Wraps the nmap/ansible commands for bootstrapping structure'
    },
    {
        header: 'Options',
        optionList: [
                {name: 'token', alias: 't', typeLabel: '[underline]{token}', description: 'REST api token'},
                {name: 'compat', alias: 'c', typeLabel: '[underline]{compat}', description: 'Robust compatibility mode'},
                {name: 'hostname', alias: 'h', typeLabel: '[underline]{hostname}', description: 'The hostname of the structure machine'}
            ]
    }
];
const usage = getUsage(sections);
const rest = new RestConnector('localhost', 3000);  //FIXME fetch from env. vars
rest.setToken(options.token);

const log = (x) => console.log(JSON.stringify(x, undefined, 2));

const main = (types) => {
    const helper = new Helper(types);

    if(options.compat) {
        let subGraph = helper.bootstrapCompatMode(options.hostname);
        log(subGraph);
        let promises = [];

        R.forEach(item => {
            promises.push(rest.createInstance({entities: [item]}));
        }, subGraph.entities);

        Promise.all(promises)
            .then(createdEntities => {
                return rest.createInstance({entities: [subGraph.os, subGraph.root]});
            })
            .then(finalResult => {
                console.log(finalResult);
                process.exit(0);
            });
    } else {
        ansibleLib.setup('localhost')
            .then((result) => {
                log(result);
                let subGraph = helper.bootstrapTransform(result, '127.0.0.1','ssh', options.hostname);
                let promises = [];

                R.forEach(item => {
                    promises.push(rest.createInstance({entities: [item]}));
                }, subGraph.entities);

                Promise.all(promises)
                    .then(createdEntities => {
                        return rest.createInstance({entities: [subGraph.os, subGraph.root]});
                    })
                    .then(finalResult => {
                        console.log(finalResult);
                        process.exit(0);
                    });
            })
            .catch(ex => console.log(ex));
    }
};

if(R.isNil(options.token)) {
    console.log('A REST api token is required');
    console.log(usage);
    process.exit(1);
}

if(R.isNil(options.hostname)) {
    console.log('A hostname is required');
    console.log(usage);
    process.exit(1);
}

rest.getAllTypes().then(types => main(types))
    .catch(ex => console.log('Failed to fetch all types from structure. (is your token valid?): %s', ex));