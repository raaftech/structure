"use strict";

const R = require('ramda');
const util = require('util');
const graphLib = require('./graphLib');

const notNil = R.compose(R.not, R.isNil);

class Helper {
    constructor(db) {
        this.types = db;
        this.fuzzyMatch = graphLib.findTypeByFuzzyNameMatch(this.types);
        this.typeByName = graphLib.findTypeByName(this.types);
        this.typeByUuid = graphLib.findTypeByUuid(this.types);
    }

    getTypeMap(graph) {
        const getId = R.path(['meta','uuid']);
        const getName = R.path(['data','name']);
        let map = {};
        R.forEach(item => {
            map[getId(item)] = getName(item);
        },graph);
        return map;
    }

    bootstrapCompatMode(hostname) {
        let graph = {root: {}, os: {}, entities: []};
        let root = this.createRootNode();
        root.name = hostname;
        root.desc = hostname;

        //OS
        let os = this.osData('OperatingSystem', 'x');
        let relate = graphLib.relateEntity(os, 'HAS', 'outgoing');

        //DNS
        let dns = this.dnsData(hostname, '');
        let pair = relate(dns);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));

        //default entities for structure bootstrap
        R.forEach(item => {
            pair = relate(item);
            os = R.clone(pair[0]);
            graph.entities.push(R.clone(pair[1]));
        }, this.defaultEntities());

        let tuple = graphLib.relateEntity(root, 'GROUPS', 'outgoing', os);
        graph.os = tuple[1];
        graph.root = tuple[0];

        return graph;
    }

    bootstrapTransform(data, ip, sshd, hostname) {
        let graph = {root: {}, os: {}, entities: []};
        let root = this.createRootNode();
        root.name = Helper.findFqdn(data);
        root.desc = util.format('%s [%s]', Helper.findFqdn(data), ip);

        //Operating System
        let os = this.osData(data.distribution, data.distribution_version);
        let relate = graphLib.relateEntity(os, 'HAS', 'outgoing');
        
        //Interpreter
        let interpreter = this.interpreterData(data.shell);
        let pair = relate(interpreter);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));

        //ServiceManager
        let manager = this.mgrData(data.service_manager);
        pair = relate(manager);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));

        //Ip data
        let ipd = this.ipData(ip, true);
        pair = relate(ipd);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));
        R.forEach(item => {
            pair = relate(item);
            os = R.clone(pair[0]);
            graph.entities.push(R.clone(pair[1]));
        }, this.harvestIps(data,ip));
        //DNS
        let dns = this.dnsData(hostname, ip);
        pair = relate(dns);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));
        //Daemons (ssh only atm)
        let daemonList = this.daemonData(sshd.service);
        R.forEach(item => {
            pair = relate(item);
            os = R.clone(pair[0]);
            graph.entities.push(R.clone(pair[1]));
        }, daemonList);
        //default entities for structure bootstrap
        R.forEach(item => {
            pair = relate(item);
            os = R.clone(pair[0]);
            graph.entities.push(R.clone(pair[1]));
        }, this.defaultEntities());

        let tuple = graphLib.relateEntity(root, 'GROUPS', 'outgoing', os);
        graph.os = tuple[1];
        graph.root = tuple[0];
    
        return graph;
    }

    osData(dist, version) {
        let c = this.fuzzyMatch(dist);
        if(R.isNil(c)) {
            c = this.typeByName('OperatingSystem');
        }
        let mkInstance = graphLib.createInstance(c);
        let result = mkInstance({
                    name: c.name,
                    desc: c.desc,
                    ver: R.defaultTo('x', version),
                    shortName: c.name,
                    longName: c.name
                });
        return result;
    }

    interpreterData(interpreter) {
        let genericInterpreter = this.typeByName('Interpreter');
        let mkInstance = graphLib.createInstance(genericInterpreter);
        let splitter = R.pipe(R.split('/'), R.last);
        let instance;

        if(R.isNil(interpreter)) {
            instance = mkInstance({
                            name: genericInterpreter.name,
                            desc: genericInterpreter.desc,
                            ver: '',
                            shortName: genericInterpreter.name,
                            longName: genericInterpreter.name
                        });
        } else {
            instance = this.fuzzyMatch(splitter(interpreter));
            if(R.isNil(instance)) {
                instance = mkInstance({
                                name: genericInterpreter.name,
                                desc: genericInterpreter.desc,
                                ver: '',
                                shortName: genericInterpreter.name,
                                longName: genericInterpreter.name
                            });
            } else {
                instance = graphLib.createInstance(instance,{
                                name: instance.name,
                                desc: instance.desc,
                                ver: '',
                                shortName: instance.name,
                                longName: instance.name
                            });
            }
        }
        
        return instance;
    }

    mgrData(mgr) {
        let c = this.fuzzyMatch(mgr);
        if(R.isNil(c)) {
            c = this.typeByName('ServiceManager');
        }
        let mkInstance = graphLib.createInstance(c);
        let result = mkInstance({
            name: c.name,
            desc: c.desc,
            ver: '',
            shortName: c.name,
            longName: c.name
        });
        return result;
    }

    daemonData(sshd) {
        let mkInstance = graphLib.createInstance(this.typeByName('SshServer'));
        if(sshd === 'ssh') sshd = 'SshServer';
        if(R.isNil(sshd)) sshd = 'none';

        let ssh = this.typeByName(sshd);
        let list = [];

        if(notNil(ssh) && sshd !== 'none') {
            ssh = mkInstance({
                name: ssh.name,
                desc: ssh.desc,
                ver: '',
                shortName: ssh.name,
                longName: ssh.name
            });
            list.push(ssh);
        } else if(R.isNil(ssh) && sshd !== 'none') {
            ssh = mkInstance({
                name: sshd,
                desc: ssh.desc,
                ver: '',
                shortName: ssh.name,
                longName: ssh.name
            });
            list.push(ssh);
        }

        return list;
    }

    //DnsName
    dnsData(dns, ip) {
        let mkInstance = graphLib.createInstance(this.typeByName('DnsName'));
        let dnsd = mkInstance({
            name: 'DnsName',
            desc: 'Either a short name or a fully-qualified domain name.',
            dnsname: R.defaultTo(ip, dns)
        });
        return dnsd;
    }

    //Ipv4Address
    ipData(ip, reachable) {
        let mkInstance = graphLib.createInstance(this.typeByName('Ipv4Address'));
        let ipd = mkInstance({
            name: 'Ipv4Address',
            desc: 'An IPv4 address',
            addr: ip,
            reachable: reachable
        });
        return ipd;
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

        return result;
    }

    //Default assumed entities
    defaultEntities() {
        const mkData = (name,desc,ver,short, long) => {
            return R.zipObj(['name','desc','ver','shortName','longName'],
                            [name,desc,ver,short,long]);
        };

        const genInstance = (name) => {
            let t = this.typeByName(name);
            let i = graphLib.createInstance(t, mkData(t.name, t.desc, 'x', t.name,t.name));
            return i;
        };

        return [
            genInstance('Iputils'),
            genInstance('OpenSshClient'),
            genInstance('Winexe'),
            genInstance('SambaClient'),
            genInstance('Structure')
        ];
    }

    createRootNode() {
        let mkInstance = graphLib.createInstance(graphLib.findTypeByName(this.types, 'ComponentGroup'));

        let computer = mkInstance({name: 'Computer', desc: 'A computer'});
        return computer;
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
}

module.exports = Helper;
