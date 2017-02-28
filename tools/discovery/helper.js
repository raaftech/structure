"use strict";

const R = require('ramda');
const util = require('util');
const Entity = require('./entity');

const notNil = R.compose(R.not, R.isNil);

class Helper {
    constructor(db) {
        this.db = db;
    }

    getConcept(name) {
        if(name === 'unknown') return null;
        return R.defaultTo(null, this.db.match(name));
    }

    transform(data, ip, hostname, sshd, httpd) {
        let os = this.osData(data.distribution, data.distribution_version,hostname,ip);
        console.log('found OS');
        let interpreter = this.interpreterData(data.shell);
        console.log('interpreter found');
        let manager = this.mgrData(data.service_manager);
        console.log('service manager found');
        let ipd = this.ipData(ip, true);
        console.log('ip data found');
        let dns = this.dnsData(data.nodename, ip);
        console.log('dns data found');
        let daemons = this.daemonData(sshd, httpd, ip);
        console.log('done');

        //TODO create the combining instance
        let ents = R.concat([os,interpreter,manager,ipd,dns], daemons);
        return new Entity.SubGraph(ents);
    }

    osData(dist, version, hostname, ip) {
        hostname = R.defaultTo('unknown-'+ip, hostname);
        let c = this.getConcept(dist);
        if(R.isNil(c)) {
            return this.getConcept('OperatingSystem');
        }
        return c;
    }

    interpreterData(interpreter) {
        let fn = R.pipe(R.split('/'), R.last);
        let c = this.getConcept(fn(interpreter));
        if(R.isNil(c)) {
            return this.getConcept('Interpreter');
        }
        return c;
    }

    mgrData(mgr) {
        let c = this.getConcept(mgr);
        if(R.isNil(c)) {
            return this.getConcept('ServiceManager');
        }
        return c;
    }

    daemonData(sshd, httpd, ip) {
        let ssh = this.getConcept(sshd);
        let http = this.getConcept(httpd);
        let list = [];
        if(notNil(http) && httpd !== 'none') list.push(http);

        if(notNil(ssh) && sshd !== 'none') list.push(ssh);

        return list;
    }

    //DnsName
    dnsData(dns, ip) {
        let dnsd = this.getConcept('DnsName');
        dnsd.data = {
            dnsname: dns
        };
        return dnsd;
    }

    //Ipv4Address
    ipData(ip, reachable) {
        let ipd = this.getConcept('Ipv4Address');
        ipd.data = {
            addr: ip,
            reachable: reachable
        };
        return ipd;
    }
}

const createEventData = (dataList) => {
    return {
        args: {
            instancedata: dataList
        },
        msgh: 'instantiateDiscoveredComputer'
    };
};

module.exports = Helper;