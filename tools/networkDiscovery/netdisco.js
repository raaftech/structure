"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const nmapLib = require('./nmapLib');
const Helper = require('./helper');
const RestConnector = require('./restConnector');

const rest = new RestConnector('localhost', 3000);
const log = (x) => console.log(JSON.stringify(x, undefined, 2));

const optionDefs = [
    {name: 'cidr', alias: 'c', type: String},
    {name: 'token', alias: 't', type: String},
    {name: 'net', alias: 'n', type: String},
    {name: 'args', alias: 'a', type: String}
];
const options = cmd(optionDefs);

const sections = [
    {
        header: 'structure bootstrap',
        content: 'Wraps the nmap command for discovering a network'
    },
    {
        header: 'Options',
        optionList: [
            {name: 'cidr', alias: 'c', typeLabel: '[underline]{cidr}', description: 'required argument, cidr to scan'},
            {name: 'token', alias: 't', typeLabel: '[underline]{token}', description: 'REST api token'},
            {name: 'net', alias: 'n', typeLabel: '[underline]{network name}]', description: 'Identifying network name in case of a network range'},
            {name: 'args', alias: 'a', typeLabel: '[underline]{nmap options}', description: 'arguments for the nmap command'}
        ]
    }
];
const usage = getUsage(sections);

const resolvePersistErrors = (network, list) => {
    let getStrings = R.filter(R.is(String));
    let toObject = R.map(x => JSON.parse(x));
    let filterErrors = R.compose(toObject, getStrings);
    let removeFromRelation = (network, uuid) => {
        network.meta.relations.HAS.uuids = R.reject(R.equals(uuid), network.meta.relations.HAS.uuids);
        return network;
    };
    let addRelation = (network, uuid) => {
        network.meta.relations.HAS.uuids = R.append(uuid, network.meta.relations.HAS.uuids);
        return network;
    };
    
    list = filterErrors(list);

    let result = {
        network: {},
        errors: []
    };
    R.forEach(item => {
        if(item.code === 409) {
            network = removeFromRelation(network, item.uuid);
            if(!R.contains(item.conflict, network.meta.relations.HAS.uuids)) 
                network = addRelation(network, item.conflict);
        } else {
            network = removeFromRelation(network, item.uuid);
        }
    },list);

    result.network = network;
    result.errors = R.reject(R.propEq('code', 409), list);

    return result;
};

const main = (db) => {
    let args = R.defaultTo('-p22,80', options.args);
    let cidr = R.defaultTo('localhost', options.cidr);
    const helper = new Helper(db); 
    rest.setToken(options.token);
    let network;
    let reuseId = false;

    rest.getInstanceByPredicate('cidr', cidr)
        .then(cidrInstance => {
            if(R.isEmpty(cidrInstance)) {
                console.log('CREATING A NEW NETWORK BASED ON: '+cidr);
                network = helper.createNetwork(cidr, options.net);
            } else {
                console.log('REUSING EXISTING NETWORK BASED ON: '+cidr);
                network = cidrInstance;
                reuseId = true;
            }

            nmapLib.nmapScan(cidr, args)
                .then((nmapResults) => {
                    let graph = helper.transform(network, nmapResults);
                    network = R.last(graph);
                    graph = R.init(graph);
                    
                    let promises = [];
                    R.forEach(item => {
                        promises.push(rest.createInstance({entities: [item]}));
                    }, graph);
                    log(helper.getTypeMap(graph));
                    return Promise.all(promises);
                })
                .then(result => {
                    //console.log(result);
                    let output = resolvePersistErrors(network, result);
                    log(output);
                    if(R.length(output.errors) !== 0) {
                        console.log('There are failed creations: ');
                        console.log(output.errors);
                    }
                    if(!reuseId) {
                        console.log('creating new network');
                        rest.createInstance({entities: [network]})
                            .then(createdNetwork => console.log(createdNetwork))
                            .catch(ex => console.log(ex));
                    } else {
                        rest.updateInstance({entities: [network]})
                            .then(createdNetwork => console.log(createdNetwork))
                            .catch(ex => console.log(createdNetwork));
                    }
                });
        })
        .catch(ex => console.log(ex));
};

if(R.isNil(options.cidr)) {
    console.log('A cidr is required');
    console.log(usage);
    process.exit(1);
}

if(R.isNil(options.token)) {
    console.log('A REST api token is required');
    console.log(usage);
    process.exit(1);
}

rest.getAllTypes().then(types => main(types))
    .catch(ex => console.log('Failed to fetch all types from structure. (is your token valid?): %s', ex));