"use strict";

const R = require('ramda');
const graphLib = require('./graphLib');

class Helper {
    constructor(db) {
        this.types = db;
        this.fuzzyMatch = graphLib.findTypeByFuzzyNameMatch(this.types);
        this.typeByName = graphLib.findTypeByName(this.types);
        this.typeByUuid = graphLib.findTypeByUuid(this.types);
    }

    transform(network, nmapData) {
        const self = this;
        const relate = graphLib.relateEntity(network,'HAS', 'outgoing');
        const mkIp = (ip) => self.createIpAddress(ip, true);
        let fn = R.compose(R.nth(1), relate, mkIp, R.prop('ip'));

        let graph = R.map(fn, nmapData);
        graph.push(network);

        return graph;
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

    //Ipv4Address
    createIpAddress(ip, reachable) {
        let mkInstance = graphLib.createInstance(this.typeByName('Ipv4Address'));
        let ipd = mkInstance({
            name: 'Ipv4Address',
            desc: 'An IPv4 address',
            addr: ip,
            reachable: reachable
        });
        ipd.name = ip;
        ipd.type = 'Component';
        return ipd;
    }

    //Ipv4Network
    createNetwork(cidr, netName) {
        let mkInstance = graphLib.createInstance(this.typeByName('Ipv4Network'));
        let ipTest = R.test(/^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/);
        let isIp = R.compose(ipTest, R.trim);
        if(isIp(cidr)) cidr = cidr + '/32';
        let network = mkInstance({
            name: R.defaultTo('Ipv4Network', netName),
            desc: 'An Ipv4Network',
            cidr: cidr
        });
        network.name = R.defaultTo('Ipv4Network', netName);
        network.type = 'Component';
        return network;
    }
}

module.exports = Helper;
