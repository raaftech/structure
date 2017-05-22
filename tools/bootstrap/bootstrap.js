"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const ansibleLib = require('./ansibleLib');
const Helper = require('./helper');
const serviceHelper = require('./serviceHelper');
const Entity = require('./entity');
const RestConnector = require('./restConnector');
const graphLib = require('./graphLib');

const optionDefs = [{name: 'token', alias: 't', type: String}];
const options = cmd(optionDefs);

const sections = [
    {
        header: 'structure bootstrap',
        content: 'Wraps the nmap/ansible commands for bootstrapping structure'
    },
    {
        header: 'Options',
        optionList: [{name: 'token', alias: 't', typeLabel: '[underline]{token}', description: 'REST api token'}]
    }
];
const usage = getUsage(sections);
const rest = new RestConnector('localhost', 3000);  //FIXME fetch from env. vars
rest.setToken(options.token);

const log = (x) => console.log(JSON.stringify(x, undefined, 2));

const main = (types) => {
    const getSshService = serviceHelper.getService('ssh');
    const helper = new Helper(types);

    ansibleLib.setup('localhost')
                .then((result) => {
                    let subGraph = helper.bootstrapTransform(result, 'localhost','ssh');
                    log(subGraph.root);
                    log(subGraph.os);
                    let promises = [];
                    rest.createInstance({entities: [subGraph.root, subGraph.os]})
                        .then(result => {
                            console.log(result);
                            R.forEach(item => {
                                promises.push(rest.createInstance({entities: [item]}));
                            }, subGraph.entities);
                            return Promise.all(promises);
                        })
                        .then(finalResult => {
                            console.log(finalResult);
                            process.exit(0);
                        })
                })
                .catch(ex => console.log(ex));
};

if(R.isNil(options.token)) {
    console.log('A REST api token is required');
    console.log(usage);
    process.exit(1);
}

rest.getAllTypes().then(types => main(types))
    .catch(ex => console.log('Failed to fetch all types from structure. (is your token valid?)'));