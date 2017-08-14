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

    transform(ansibleData, ipEntity) {
        let graph = {root: {}, os: {}, entities: []};
        let ipString = R.path(['data','addr'],ipEntity);

        let root = this.createRootNode();
        root.name = Helper.findFqdn(ansibleData);
        root.desc = util.format('%s [%s]', Helper.findFqdn(ansibleData), ipString);

        //Operating System
        let os = this.osData(ansibleData.distribution, ansibleData.distribution_version);
        let relate = graphLib.relateEntity(os, 'HAS', 'outgoing');
        //Interpreter
        let interpreter = this.interpreterData(ansibleData.shell);
        let pair = relate(interpreter);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));

        //ServiceManager
        let manager = this.mgrData(ansibleData.service_manager);
        pair = relate(manager);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));

        //Ip data
        pair = relate(ipEntity);
        os = R.clone(pair[0]);
//        graph.entities.push(R.clone(pair[1]));
//        R.forEach(item => {
//            pair = relate(item);
//            os = R.clone(pair[0]);
//            graph.entities.push(R.clone(pair[1]));
//        }, this.harvestIps(ansibleData,ipString));

        //SSH / SMB
        let ssh = this.daemonData();
        pair = relate(ssh);
        os = R.clone(pair[0]);
        graph.entities.push(R.clone(pair[1]));

        let tuple = graphLib.relateEntity(root, 'MEMBER_OF', 'incoming', os);
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
        let server = this.typeByName('SshServer');
        let mkInstance = graphLib.createInstance(server);
        let ssh = mkInstance({
            name: server.name,
            desc: server.desc,
            ver: '',
            shortName: server.name,
            longName: server.name
        });

        return ssh;
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
