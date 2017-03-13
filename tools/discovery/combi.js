"use strict";

const R = require("ramda");
const async = require('async');
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const nmapLib = require('./nmapLib');
const ansibleLib = require('./ansibleLib');
const vClient = require('./vertxClient');
const Helper = require('./helper');
const serviceHelper = require('./serviceHelper');
const Entity = require('./entity');

const optionDefs = [
    {name: 'cidr', alias: 'c', type: String},
    {name: 'args', alias: 'a', type: String},
    {name: 'verbose', alias: 'v', type: Boolean}
];
const options = cmd(optionDefs);

const sections = [
    {
        header: 'structure wrapper',
        content: 'Wraps the nmap/ansible commands for json output for structure'
    },
    {
        header: 'Options',
        optionList: [
            {name: 'cidr', alias: 'c', typeLabel: '[underline]{cidr}', description: 'required argument, cidr to scan'},
            {name: 'args', alias: 'a', typeLabel: '[underline]{nmap options}', description: 'arguments for the nmap command'},
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
    let client = new vClient();

    let network = helper.createNetwork();
    let networkRelation = network.relationCreate('HAS_COMPONENT', 'outgoing');

    nmapLib.nmapScan(cidr, args)
        .then((nmapResults) => {
            const count = R.length(nmapResults);
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
                        let subGraph = helper.transform(result, item.ip,
                                                   getSshService(nmapResults[index]),
                                                   getHttpService(nmapResults[index]),
                                                   network,
                                                   networkRelation);

                        //if(options.verbose) Helper.prettyPrint(subGraph);
                        //if(options.verbose) console.log(JSON.stringify(subGraph.toList(),undefined,2));
                        // client.createGraph({msgh: "create", args: {entities: subGraph.toList()}})
                        //     .then((created) => {
                        //         console.log('Processing result %s of %s for %s [%s]', index, count, nmapResults[index].hostname, nmapResults[index].ip);
                        //         console.log('------CREATE result');
                        //         console.log(created.body.result.data);
                        //         console.log('------');
                        //         doneCalled = true;
                        //         done();
                        //     });
                        networkRelation.addEntity(subGraph);
                        doneCalled = true;
                        done();
                    })
                    .catch(ex => console.log(ex));
            }, function(err) {
                if(err) console.log('Ended with an error: '+err);
                console.log('Done running discovery of %s', cidr);

                let finalGraph = new Entity.SubGraph(network);
                //console.log(JSON.stringify(finalGraph.toList(),undefined,2));
                R.forEach(item => {
                    client.createGraph({msgh: "create", args: {entities: [item]}})
                        .then((created) => {
                            //console.log('Processing result %s of %s for %s [%s]', index, count, nmapResults[index].hostname, nmapResults[index].ip);
                            // console.log('------CREATE result');
                            // console.log(created.body.result.data);
                            // console.log('------');
                        });
                }, finalGraph.toList());

                client.close();
                process.exit(0);
            });
        });
};

if(R.isNil(options.cidr)) {
    console.log(usage);
    process.exit(0);
}

let client = new vClient();
client.getAllTypes().then(typeDB => {
    main(typeDB);
});