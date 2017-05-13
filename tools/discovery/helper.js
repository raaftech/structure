"use strict";

const R = require('ramda');
const util = require('util');
const Entity = require('./entity');
const treeify = require('treeify');
const uuidV4 = require('uuid');

const notNil = R.compose(R.not, R.isNil);

class Helper {
    constructor(db) {
        this.db = db;
    }

    getInstance(name) {
        if(name === 'unknown') return null;
        return R.defaultTo(null, Helper.json2Entity(this.db.match(name),true));
    }

    getType(name) {
        if(name === 'unknown') return null;
        return R.defaultTo(null, Helper.json2Entity(this.db.match(name), false));
    }

    discoTransform(data, ip, sshd, httpd) {
        let root = this.createRootNode();
        root.name = Helper.findFqdn(data);
        root.desc = util.format('%s [%s]', Helper.findFqdn(data), ip);

        let groupsRelation = root.relationCreate('GROUPS', 'outgoing');

        //Operating System
        let os = this.osData(data.distribution, data.distribution_version);
        //rootRelation.addEntity(os);
        groupsRelation.addEntity(os);
        let osHasRelation = os.relationCreate('HAS', 'outgoing');

        //Interpreter
        let interpreter = this.interpreterData(data.shell);
        osHasRelation.addEntity(interpreter);
        //ServiceManager
        let manager = this.mgrData(data.service_manager);
        osHasRelation.addEntity(manager);
        //Ip data
        let ipd = this.ipData(ip, true);
        osHasRelation.addEntity(ipd);
        R.forEach(item => osHasRelation.addEntity(item), this.harvestIps(data,ip));
        //DNS
        let dns = this.dnsData(Helper.findFqdn(data), ip);
        osHasRelation.addEntity(dns);
        //Daemons (http, ssh only atm)
        let daemonList = this.daemonData(sshd.service, httpd.service);
        R.forEach(item => osHasRelation.addEntity(item), daemonList);

        return root;
    }

    bootstrapTransform(data, ip, sshd, httpd) {
            let root = this.createRootNode();
            root.name = Helper.findFqdn(data);
            root.desc = util.format('%s [%s]', Helper.findFqdn(data), ip);

            let groupsRelation = root.relationCreate('GROUPS', 'outgoing');

            //Operating System
            let os = this.osData(data.distribution, data.distribution_version);
            //rootRelation.addEntity(os);
            groupsRelation.addEntity(os);
            let osHasRelation = os.relationCreate('HAS', 'outgoing');

            //Interpreter
            let interpreter = this.interpreterData(data.shell);
            osHasRelation.addEntity(interpreter);
            //ServiceManager
            let manager = this.mgrData(data.service_manager);
            osHasRelation.addEntity(manager);
            //Ip data
            let ipd = this.ipData(ip, true);
            osHasRelation.addEntity(ipd);
            R.forEach(item => osHasRelation.addEntity(item), this.harvestIps(data,ip));
            //DNS
            let dns = this.dnsData(Helper.findFqdn(data), ip);
            osHasRelation.addEntity(dns);
            //Daemons (http, ssh only atm)
            let daemonList = this.daemonData(sshd.service, httpd.service);
            R.forEach(item => osHasRelation.addEntity(item), daemonList);

            R.forEach(item => osHasRelation.addEntity(item), this.defaultEntities());

            return root;
        }

    osData(dist, version) {
        let c = this.getInstance(dist);
        if(R.isNil(c)) {
            let gc = this.getInstance('OperatingSystem');
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
            let gc = this.getInstance('Interpreter');
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
        let c = this.getInstance(fn(interpreter));
        if(R.isNil(c)) {
            let gc = this.getInstance('Interpreter');
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
        let c = this.getInstance(mgr);
        if(R.isNil(c)) {
            let gc = this.getInstance('ServiceManager');
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

        if(R.isNil(sshd)) sshd = 'none';
        if(R.isNil(httpd)) httpd = 'none';

        console.log('ssh: %s , http: %s', sshd, httpd);
        let ssh = this.getInstance(sshd);
        //ssh = Helper.json2Entity( R.dissocPath(['meta','relations','IMPLEMENTS'], ssh), false );
        ssh = this.cleanEntity(ssh, ['IMPLEMENTS', 'CONSTRAINTS']);
        let http = this.getInstance(httpd);
        //http = Helper.json2Entity( R.dissocPath(['meta','relations','IMPLEMENTS'], http), false );
        http = this.cleanEntity(http, ['IMPLEMENTS', 'CONSTRAINTS']);
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
            http = this.getInstance('HttpServer');
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
        } else if(R.isNil(ssh) && sshd !== 'none') {
            ssh = this.getInstance('SshServer');
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
        let dnsd = this.getInstance('DnsName');
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
        let ipd = this.getInstance('Ipv4Address');
        ipd.data = {
            name: ipd.name,
            desc: ipd.desc,
            addr: ip,
            reachable: reachable
        };
        ipd.type = 'Configuration';
        return this.createInstance(ipd,'Ipv4Address');
    }

    harvestIps(ansibleData,initIp) {
        let result = [];
        if(notNil(ansibleData.ipv4_addresses)) {
            R.forEach(ip => {
                if(!R.equals(ip, initIp)) {
                    result.push(this.ipData(ip, true));
                }
            }, ansibleData.ipv4_addresses);
        }

        if(notNil(ansibleData.ipv6_addresses)) {
            R.forEach(ip => {
                if(!R.equals(ip, initIp)) {
                    let ipd = this.getInstance('Ipv6Address');
                    ipd.data = {
                        name: ipd.name,
                        desc: ipd.desc,
                        addr: ip,
                        reachable: true
                    };
                    ipd.type = 'Configuration';
                    result.push(this.createInstance(ipd,'Ipv6Address'));
                }
            }, ansibleData.ipv6_addresses);
        }

        return result;
    }

    //Default assumed entities
    defaultEntities() {
        const mkData = (name,desc,ver,short, long) => {
            return R.zipObj(['name','desc','ver','short','long'],
                            [name,desc,ver,short,long]);
        };

        let iputils = this.getInstance('Iputils');
        iputils.type = 'Component';
        iputils.data = mkData(iputils.name, iputils.desc, 'x', 'iputils', 'IpUtils');

        let sshClient = this.getInstance('OpenSshClient');
        sshClient.type = 'Component';
        sshClient.data = mkData(sshClient.name, sshClient.desc, 'x', 'sshclient', 'SshClient');

        let winexe = this.getInstance('Winexe');
        winexe.type = 'Component';
        winexe.data = mkData(winexe.name, winexe.desc, 'x', 'winexe', 'WinExe');

        let samba = this.getInstance('Samba');
        samba.type = 'Component';
        samba.data = mkData(samba.name, samba.desc, 'x', 'samba', 'Samba');

        let structure = this.getInstance('Structure');
        structure.type = 'Component';
        structure.data = mkData(structure.name, structure.desc, '1.0', 'structure', 'Structure' );

        return [
            this.createInstance(iputils, 'Iputils'),
            this.createInstance(sshClient, 'OpenSshClient'),
            this.createInstance(winexe, 'Winexe'),
            this.createInstance(samba, 'Samba'),
            this.createInstance(structure, 'Structure')
        ];
    }


    createInstance(entity, instanceName) {
        let i = this.getType(instanceName);
        let rel = entity.relationCreate('INSTANCE_OF', 'outgoing');
        rel.addEntity(i);
        entity = this.cleanEntity(entity, ['IMPLEMENTS', 'CONSTRAINTS']);
        return entity;
    }

    createRootNode() {
        let computer = this.getInstance('Computer');
        computer.type = 'Grouping';
        let rel = computer.relationCreate('INSTANCE_OF', 'outgoing');
        rel.addEntity(this.getType('Computer'));
        computer = this.cleanEntity(computer, ['IMPLEMENTS', 'CONSTRAINTS']);
        return computer;
    }

    createNetwork(cidr, netName) {
        let network = this.getInstance('Ipv4Network');
        network.type = 'Grouping';

        let ipTest = R.test(/^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/);
        let isIp = R.compose(ipTest, R.trim);
        if(isIp(cidr)) cidr = cidr + '/32';

        network.name = R.defaultTo(network.name, netName);
        network.data = {
            name: R.defaultTo(network.name, netName),
            desc: network.desc,
            cidr: cidr
        };
        let rel = network.relationCreate('INSTANCE_OF', 'outgoing');
        rel.addEntity(this.getType('Ipv4Network'));
        network = this.cleanEntity(network, ['IMPLEMENTS', 'CONSTRAINTS']);
        return network;
    }

    cleanEntity(entity,rels) {
        if(R.isNil(entity)) return entity;

        R.forEach(rel => {
            entity = Helper.json2Entity( R.dissocPath(['meta','relations',rel], entity), false );
        }, rels);
        return entity;
    }

    static findFqdn(ansibleData) {
        let fqdn = R.prop('fqdn', ansibleData);
        let hostname = R.prop('hostname', ansibleData);
        let nodename = R.prop('nodename', ansibleData);

        let emptyOrNil = (x) => R.or(R.isNil(x), R.isEmpty(x));
        let isAvailable = R.compose(R.not, emptyOrNil);

        //TODO find a better way to solve this
        if(fqdn === 'localhost') {
            return nodename;
        }

        let fn = R.cond([
            [x => isAvailable(fqdn), x => R.identity(fqdn)],
            [x => isAvailable(hostname), x => R.identity(hostname)],
            [x => isAvailable(nodename), x => R.identity(nodename)],
            [R.T, R.always('unknown')]
        ]);

        return fn();
    }

    static json2Entity(json, generateUuid) {
        if(R.isNil(json)) return null;
        let defaultUuid = R.ifElse(R.identity, x => uuidV4(), x => R.defaultTo(null, json.meta.uuid))(generateUuid);
        let defaultRelations = R.ifElse(R.identity,
                            x => R.ifElse(notNil, R.omit(['ISA']), R.identity)(R.defaultTo(null, R.clone(json.meta.relations))),
                            x => R.defaultTo(null, R.clone(json.meta.relations)))(generateUuid);

        return new Entity.Entity(
            R.defaultTo(null, json.type),
            R.defaultTo(null, json.name),
            R.defaultTo(null, json.desc),
            defaultUuid,
            R.defaultTo(null, R.clone(json.data)),
            defaultRelations,
            R.defaultTo(null, R.clone(json.file))
        );
    }
}

module.exports = Helper;