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
                        //console.log('got ansible result for %s [%s]', nmapResults[index].hostname, nmapResults[index].ip);
                        if(options.verbose) console.log(JSON.stringify(result,undefined,2));

                        let subGraph = helper.transform(result, item.ip, item.hostname,
                                                   getSshService(nmapResults[index]),
                                                   getHttpService(nmapResults[index]));

                        if(options.verbose) console.log(JSON.stringify(subGraph.toList(), undefined, 2));
                        client.send(subGraph.toList());
                        console.log('Processing result %s of %s for %s [%s]', index, count, nmapResults[index].hostname, nmapResults[index].ip);
                        console.log('------');
                        doneCalled = true;
                        done();
                    });
            }, function(err) {
                if(err) console.log('Ended with an error: '+err);
                console.log('Done running discovery of %s', cidr);
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
//    let helper = new Helper(typeDB);
//    let mock = {
//        distribution: 'FreeBSD',
//        distribution_version: '11.0',
//        shell: '/usr/bin/bash',
//        service_manager: 'bsdinit',
//        nodename: 'soekris.raaf.local'
//    };
//    let result = helper.transform(mock, '172.17.1.1', 'soekris.raaf.local','sshd', 'apache');
//    console.log(JSON.stringify(result, undefined,2));
});