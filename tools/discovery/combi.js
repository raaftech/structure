"use strict";

const R = require("ramda");
const async = require('async');
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const nmapLib = require('./nmapLib');
const ansibleLib = require('./ansibleLib');
const vClient = require('./vertxClient');
const helper = require('./helper');

const serviceMap = [
    {ip: '172.17.1.1', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.5', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.18', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.19', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.26', ssh: 'none', httpd: 'debut'},
    {ip: '172.17.1.40', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.41', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.49', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.50', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.51', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.52', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.54', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.56', ssh: 'winssh', httpd: 'w3svc'},
    {ip: '172.17.1.58', ssh: 'openssh', httpd: 'org.apache.httpd'},
    {ip: '172.17.1.59', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.60', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.61', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.62', ssh: 'openssh', httpd: 'httpd'},
    {ip: '172.17.1.63', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.64', ssh: 'ssh', httpd: 'none'},
    {ip: '172.17.1.65', ssh: 'openssh', httpd: 'httpd'},
    {ip: '172.17.1.67', ssh: 'sunssh', httpd: 'none'},
    {ip: '172.17.1.68', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.69', ssh: 'sunssh', httpd: 'http'},
    {ip: '172.17.1.70', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.71', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.72', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.73', ssh: 'openssh', httpd: 'apache2'},
    {ip: '172.17.1.74', ssh: 'none', httpd: 'none'},
    {ip: '172.17.1.76', ssh: 'none', httpd: 'none'},
    {ip: '172.17.1.79', ssh: 'none', httpd: 'none'},
    {ip: '172.17.1.82', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.83', ssh: 'openssh', httpd: 'none'},
    {ip: '172.17.1.85', ssh: 'none', httpd: 'none'},
    {ip: '172.17.1.253', ssh: 'dropbear', httpd: 'goahead'},
    {ip: '172.17.1.254', ssh: 'none', httpd: 'none'}
];

const getMapByIp = (ip) => {
    let x = R.find(R.propEq('ip', ip), serviceMap);
    if(R.isNil(x)) {
        return {ip: ip, ssh: 'none', httpd: 'none'};
    }
    return x;
};

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

const main = () => {
    if(R.isNil(options.cidr)) {
        console.log(usage);
        process.exit(0);
    }

    let args = R.defaultTo('-p22,80', options.args);
    let cidr = R.defaultTo('localhost', options.cidr);
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
                        console.log('got ansible result for %s [%s]', nmapResults[index].hostname, nmapResults[index].ip);
                        if(options.verbose) console.log(JSON.stringify(result,undefined,2));
                        let serviceInfo = getMapByIp(item.ip);
                        let buf = helper.transform(result,item.ip, item.hostname, serviceInfo.ssh, serviceInfo.httpd);
                        if(options.verbose) console.log(JSON.stringify(buf, undefined, 2));
                        client.send(buf);
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

main();
