"use strict";

const R = require('ramda');
const util = require('util');
const Entity = require('./entity');
const treeify = require('treeify');

const notNil = R.compose(R.not, R.isNil);

class Helper {
    constructor(db) {
        this.db = db;
    }

    getConcept(name) {
        if(name === 'unknown') return null;
        return R.defaultTo(null, Helper.json2Entity(this.db.match(name)));
    }

    transform(data, ip, sshd, httpd) {
        let root = this.createRootNode();
        let rootHasComponentRelation = root.relationCreate('HAS_COMPONENT', 'outgoing');
        let rootHasConfigurationRelation = root.relationCreate('HAS_CONFIGURATION', 'outgoing');

        //Operating System
        let os = this.osData(data.distribution, data.distribution_version);
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
        let daemonList = this.daemonData(sshd.service, httpd.service);
        R.forEach(item => rootHasComponentRelation.addEntity(item), daemonList);
        return new Entity.SubGraph(root);
    }

    osData(dist, version) {
        let c = this.getConcept(dist);
        if(R.isNil(c)) {
            let gc = this.getConcept('OperatingSystem');
            gc.type = 'Component';
            gc.data = {
                name: gc.name,
                desc: gc.desc,
                ver: '',
                shortName: gc.name,
                longName: gc.name
            };
            return this.createInstance(gc,'OperatingSystem');
        }
        c.type = 'Component';
        c.data = {
            name: c.name,
            desc: c.desc,
            ver: version,
            shortName: c.name,
            longName: c.name
        };
        return this.createInstance(c, dist);
    }

    interpreterData(interpreter) {
        if(R.isNil(interpreter)) {
            let gc = this.getConcept('Interpreter');
            gc.type = 'Component';
            gc.data = {
                name: gc.name,
                desc: gc.desc,
                ver: '',
                shortName: gc.name,
                longName: gc.name
            };
            return this.createInstance(gc, 'Interpreter');
        }
        let fn = R.pipe(R.split('/'), R.last);
        let c = this.getConcept(fn(interpreter));
        if(R.isNil(c)) {
            let gc = this.getConcept('Interpreter');
            gc.type = 'Component';
            gc.data = {
                name: gc.name,
                desc: gc.desc,
                ver: '',
                shortName: gc.name,
                longName: gc.name
            };
            return this.createInstance(gc, 'Interpreter');
        }
        c.type = 'Component';
        c.data = {
            name: c.name,
            desc: c.desc,
            ver: '',
            shortName: c.name,
            longName: c.name
        };
        return this.createInstance(c,fn(interpreter))
    }

    mgrData(mgr) {
        let c = this.getConcept(mgr);
        if(R.isNil(c)) {
            let gc = this.getConcept('ServiceManager');
            gc.type = 'Component';
            gc.data = {
                name: gc.name,
                desc: gc.desc,
                ver: '',
                shortName: gc.name,
                longName: gc.name
            };
            return this.createInstance(gc, 'ServiceManager');
        }
        c.type = 'Component';
        c.data = {
            name: c.name,
            desc: c.desc,
            ver: '',
            shortName: c.name,
            longName: c.name
        };
        return this.createInstance(c, mgr);
    }

    daemonData(sshd, httpd) {
        if(sshd === 'ssh') sshd = 'sshserver';
        if(httpd === 'http') httpd = 'httpserver';

        let ssh = this.getConcept(sshd);
        let http = this.getConcept(httpd);
        let list = [];

        if(notNil(http) && httpd !== 'none') {
            http.type = 'Component';
            http.data = {
                name: http.name,
                desc: http.desc,
                ver: '',
                shortName: http.name,
                longName: http.name
            };
            list.push(this.createInstance(http, httpd));
        } else if(R.isNil(http) && httpd !== 'none') {
            http = this.getConcept('HttpServer');
            http.type = 'Component';
            http.data = {
                name: httpd,
                desc: http.desc,
                ver: '',
                shortName: http.name,
                longName: http.name
            };
            list.push(this.createInstance(http, 'HttpServer'));
        }

        if(notNil(ssh) && sshd !== 'none') {
            ssh.type = 'Component';
            ssh.data = {
                name: ssh.name,
                desc: ssh.desc,
                ver: '',
                shortName: ssh.name,
                longName: ssh.name
            };
            list.push(this.createInstance(ssh, sshd));
        }else if(R.isNil(ssh) && sshd !== 'none') {
            ssh = this.getConcept('SshServer');
            ssh.type = 'Component';
            ssh.data = {
                name: sshd,
                desc: ssh.desc,
                ver: '',
                shortName: ssh.name,
                longName: ssh.name
            };
            list.push(this.createInstance(ssh, 'SshServer'));
        }

        return list;
    }

    //DnsName
    dnsData(dns, ip) {
        let dnsd = this.getConcept('DnsName');
        dnsd.type = 'Configuration';
        dnsd.data = {
            name: dnsd.name,
            desc: dnsd.desc,
            dnsname: R.defaultTo(ip, dns)
        };
        return this.createInstance(dnsd, 'DnsName');
    }

    //Ipv4Address
    ipData(ip, reachable) {
        let ipd = this.getConcept('Ipv4Address');
        ipd.data = {
            name: ipd.name,
            desc: ipd.desc,
            addr: ip,
            reachable: reachable
        };
        ipd.type = 'Configuration';
        return this.createInstance(ipd,'Ipv4Address');
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

    static json2Entity(json) {
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

    static prettyPrint(graph) {
        let list = graph.toList();
        list = R.map(item => {
            return {
                type: item.type,
                name: item.name,
                desc: item.desc,
                meta: R.clone(item.meta),
                data: R.clone(item.data)
            };
        }, list);

        let root = R.find(R.propEq('name','Computer'),list);
        let components = [];
        let configs = [];

        R.forEach(uuid => {
            let comp = R.find(R.pathEq(['meta','uuid'],uuid), list);
            components.push(comp);
        }, root.meta.relations['HAS_COMPONENT'].uuids);

        R.forEach(uuid => {
            let cfg = R.find(R.pathEq(['meta','uuid'],uuid), list);
            configs.push(cfg);
        }, root.meta.relations['HAS_CONFIGURATION'].uuids);

        root.meta.relations['HAS_COMPONENT'] = components;
        root.meta.relations['HAS_CONFIGURATION'] = configs;

        let output = treeify.asTree(root, true);
        console.log(output);
    }
}

module.exports = Helper;