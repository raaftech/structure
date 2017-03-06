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
        return R.defaultTo(null, this.json2Entity(this.db.match(name)));
    }

    transform(data, ip, hostname, sshd, httpd) {
        let root = this.createRootNode();
        let rootHasComponentRelation = root.relationCreate('HAS_COMPONENT', 'outgoing');
        let rootHasConfigurationRelation = root.relationCreate('HAS_CONFIGURATION', 'outgoing');

        //Operating System
        let os = this.osData(data.distribution, data.distribution_version,hostname,ip);
        rootHasComponentRelation.addEntity(os);
        //Interpreter
        let interpreter = this.interpreterData(data.shell);
        rootHasComponentRelation.addEntity(interpreter);
        //ServiceManager
        let manager = this.mgrData(data.service_manager);
        rootHasComponentRelation.addEntity(manager);
        //Ip data
        let ipd = this.ipData(ip, true);
        rootHasConfigurationRelation.addEntity(ipd);
        //DNS
        let dns = this.dnsData(data.nodename, ip);
        rootHasConfigurationRelation.addEntity(dns);
        //Daemons (http, ssh only atm)
        let daemonList = this.daemonData(sshd, httpd);
        R.forEach(item => rootHasComponentRelation.addEntity(item), daemonList);
        return new Entity.SubGraph(root);
    }

    osData(dist, version, hostname, ip) {
        hostname = R.defaultTo('unknown-'+ip, hostname);

        let c = this.getConcept(dist);
        if(R.isNil(c)) {
            let gc = this.getConcept('OperatingSystem');
            gc.type = 'Component';
            return this.createInstance(gc,'OperatingSystem');
        }
        c.type = 'Component';
        return this.createInstance(c, dist);
    }

    interpreterData(interpreter) {
        let fn = R.pipe(R.split('/'), R.last);
        let c = this.getConcept(fn(interpreter));
        if(R.isNil(c)) {
            let gc = this.getConcept('Interpreter');
            gc.type = 'Component';
            return this.createInstance(gc, 'Interpreter');
        }
        c.type = 'Component';
        return this.createInstance(c,fn(interpreter))
    }

    mgrData(mgr) {
        let c = this.getConcept(mgr);
        if(R.isNil(c)) {
            let gc = this.getConcept('ServiceManager');
            gc.type = 'Component';
            return this.createInstance(gc, 'ServiceManager');
        }
        c.type = 'Component';
        return this.createInstance(c, mgr);
    }

    daemonData(sshd, httpd) {
        let ssh = this.getConcept(sshd);
        let http = this.getConcept(httpd);
        let list = [];
        if(notNil(http) && httpd !== 'none') {
            http.type = 'Component';
            list.push(this.createInstance(http, httpd));
        }

        if(notNil(ssh) && sshd !== 'none') {
            ssh.type = 'Component';
            list.push(this.createInstance(ssh, sshd));
        }
        return list;
    }

    //DnsName
    dnsData(dns, ip) {
        let dnsd = this.getConcept('DnsName');
        dnsd.type = 'Configuration';
        dnsd.data = {
            dnsname: dns
        };
        return this.createInstance(dnsd, 'DnsName');
    }

    //Ipv4Address
    ipData(ip, reachable) {
        let ipd = this.getConcept('Ipv4Address');
        ipd.data = {
            addr: ip,
            reachable: reachable
        };
        ipd.type = 'Configuration';
        return this.createInstance(ipd,'Ipv4Address');
    }

    json2Entity(json) {
        if(R.isNil(json)) return null;

        return new Entity.Entity(
                    R.defaultTo(null, json.type),
                    R.defaultTo(null, json.name),
                    R.defaultTo(null, json.desc),
                    R.defaultTo(null, json.meta.uuid),
                    R.defaultTo(null, R.clone(json.data)),
                    R.defaultTo(null, R.clone(json.meta.relations)),
                    R.defaultTo(null, R.clone(json.file))
                 );
    }


    createInstance(entity, instanceName) {
        let i = this.getConcept(instanceName);
        let rel = entity.relationCreate('INSTANCE_OF', 'outgoing');
        rel.addEntity(i);
        return entity;
    }

    createRootNode() {
        let computer = this.getConcept('Computer');
        computer.type = 'Grouping';
        let rel = computer.relationCreate('INSTANCE_OF', 'outgoing');
        rel.addEntity(this.getConcept('Computer'));
        return computer;
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