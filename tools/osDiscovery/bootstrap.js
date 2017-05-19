"use strict";

const R = require("ramda");
const async = require('async');
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const nmapLib = require('./nmapLib');
const ansibleLib = require('./ansibleLib');
const Helper = require('./helper');
const serviceHelper = require('./serviceHelper');
const Entity = require('./entity');
const vClient = require('./vertxClient');
const RestConnector = require('./restConnector');
const graphLib = require('./graphLib');

const optionDefs = [
    {name: 'cidr', alias: 'c', type: String},
    {name: 'token', alias: 't', type: String},
    {name: 'net', alias: 'n', type: String},
    {name: 'args', alias: 'a', type: String},
    {name: 'bootstrap', alias: 'b', type: Boolean},
    {name: 'verbose', alias: 'v', type: Boolean}
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
            {name: 'cidr', alias: 'c', typeLabel: '[underline]{cidr}', description: 'required argument, cidr to scan'},
            {name: 'token', alias: 't', typeLabel: '[underline]{token}', description: 'REST api token'},
            {name: 'net', alias: 'n', typeLabel: '[underline]{network name}]', description: 'Identifying network name in case of a network range'},
            {name: 'args', alias: 'a', typeLabel: '[underline]{nmap options}', description: 'arguments for the nmap command'},
            {name: 'bootstrap', alias: 'b', typeLabel: '[underline]{bootstrap}', description: 'Start a structure boostrap self discovery'},
            {name: 'verbose', alias: 'v', description: 'set verbose mode'}
        ]
    }
];
const usage = getUsage(sections);

const main = (db) => {
    let args = R.defaultTo('-p22,80', options.args);
    let cidr = R.defaultTo('localhost', options.cidr);

    const getSshService = serviceHelper.getService('ssh');
    const getHttpService = serviceHelper.getService('http');
    const helper = new Helper(db);
    const rest = new RestConnector('localhost', 3000);
    rest.setToken(options.token);
    let client = new vClient();
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

            //let networkGroup = network.relationCreate('GROUPS', 'outgoing');

            nmapLib.nmapScan(cidr, args)
                .then((nmapResults) => {
                    async.eachOfSeries(nmapResults, function(item,index,done) {
                        console.log('calling ansible for %s [%s]', nmapResults[index].hostname, nmapResults[index].ip);
                        let doneCalled = false;
                        setTimeout(() => {
                            if(!doneCalled) {
                                console.log('Ansible query timed out for %s [%s]', nmapResults[index].hostname, nmapResults[index].ip);
                                console.log('------');
                                done();
                            }
                        }, 15000);
                        ansibleLib.setup(item.ip)
                            .then((result) => {
                                result.fqdn = nmapResults[index].hostname;
                                let subGraph;
                                if(options.bootstrap) {
                                    subGraph = helper.bootstrapTransform(result, item.ip,
                                                       getSshService(nmapResults[index]),
                                                       getHttpService(nmapResults[index]));
                                } else {
                                    subGraph = helper.discoTransform(result, item.ip,
                                                       getSshService(nmapResults[index]),
                                                       getHttpService(nmapResults[index]));
                                }

                                //networkGroup.addEntity(subGraph);
                                let b = graphLib.relateEntity(network,'HAS', 'outgoing', );
                                doneCalled = true;
                                done();
                            })
                            .catch(ex => console.log(ex));
                    }, function(err) {
                        if(err) console.log('Ended with an error: '+err);
                        console.log('Done running discovery of %s', cidr);

                        let finalGraph = new Entity.SubGraph(network);

                        if(reuseId === '') {
                            let promises = [];
                            R.forEach(item => {
                                promises.push(rest.createInstance({entities: [item]}));
                            }, finalGraph.toList());
                            Promise.all(promises)
                                .then(result => {
                                    console.log(result);
                                    client.close();
                                    process.exit(0);
                                });
                        } else {
                            let promises = [];
                            R.forEach(item => {
                                if(item.meta.uuid === reuseId) {
                                    console.log('====================LEAVING OUT REUSED network================');
                                    //promises.push(rest.updateInstance({entities: [item]}));
                                } else {
                                    promises.push(rest.createInstance({entities: [item]}));
                                }
                            }, finalGraph.toList());
                            Promise.all(promises)
                                .then(result => {
                                    console.log(result);

                                    let ps = [];
                                    R.forEach(item => {
                                        if(item.type === 'Grouping' && item.meta.uuid !== reuseId) {
                                            ps.push(rest.addToGroup(reuseId, item.meta.uuid));
                                        }
                                    }, finalGraph.toList());

                                    Promise.all(ps)
                                        .then(grouped => {
                                            console.log(grouped);
                                            client.close();
                                            process.exit(0);
                                        });
                                });
                        }
                    });
                });
        })
        .catch(ex => console.log(ex));
};

if(R.isNil(options.token)) {
    console.log('A token is required');
    process.exit(1);
}

if(R.isNil(options.cidr)) {
    console.log(usage);
    process.exit(0);
}

let client = new vClient();
client.getAllTypes().then(typeDB => {
    main(typeDB);
});