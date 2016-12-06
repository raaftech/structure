"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const nmapLib = require('./nmapLib');
const ansibleLib = require('./ansibleLib');
const vClient = require('./vertxClient');
const helper = require('./helper');

const optionDefs = [
    {name: 'cidr', alias: 'c', type: String},
    {name: 'args', alias: 'a', type: String}
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
            {name: 'args', alias: 'a', typeLabel: '[underline]{nmap options}', description: 'arguments for the nmap command'}
        ]
    }
];
const usage = getUsage(sections);

const main = () => {
    if(R.isNil(options.cidr)) {
        console.log(usage);
        process.exit(0);
    }

    let args = R.defaultTo('-p22,80', options.args);
    let cidr = R.defaultTo('localhost', options.cidr);
    let client = new vClient();
    let count = 0;

    nmapLib.nmapScan(cidr, args)
        .then((nmapResults) => {
            let ips = R.pluck('ip', nmapResults);
            count = R.length(ips);

            R.forEach((ip) => {
                ansibleLib.setup(ip)
                    .then((result) => {
                        let buf = helper.transform(result);
                        console.log(JSON.stringify(buf, undefined, 2));
                        client.send(buf);
                        count--;
                        console.log('[%s] %s', count,ip);
                    });
            }, ips);
        });
};

main();