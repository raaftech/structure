"use strict";

const R = require("ramda");
const cmd = require('command-line-args');
const getUsage = require('command-line-usage');
const ansibleLib = require('./ansibleLib');
const Helper = require('./helper');
const RestConnector = require('./restConnector');

const rest = new RestConnector('localhost', 3000);
const log = (x) => console.log(JSON.stringify(x, undefined, 2));

const optionDefs = [
    {name: 'token', alias: 't', type: String},
    {name: 'ip', alias: 'i', type: String}
];
const options = cmd(optionDefs);

const sections = [
    {
        header: 'Ansible OS discovery',
        content: 'Wraps the ansible command for discovering os setup for an ipv4address'
    },
    {
        header: 'Options',
        optionList: [
            {name: 'token', alias: 't', typeLabel: '[underline]{token}', description: 'REST api token'},
            {name: 'ip', alias: 'i', typeLabel: '[underline]{ip}', description: 'The IP address to scan with ansible'}
        ]
    }
];
const usage = getUsage(sections);

const resolvePersistErrors = (os, list) => {
    let getStrings = R.filter(R.is(String));
    let toObject = R.map(x => JSON.parse(x));
    let filterErrors = R.compose(toObject, getStrings);
    let removeFromRelation = (osEntity, uuid) => {
        osEntity.meta.relations.HAS.uuids = R.reject(R.equals(uuid), osEntity.meta.relations.HAS.uuids);
        return osEntity;
    };
    let addRelation = (osEntity, uuid) => {
        osEntity.meta.relations.HAS.uuids = R.append(uuid, osEntity.meta.relations.HAS.uuids);
        return osEntity;
    };
    list = filterErrors(list);
    let result = {os: {}, errors: []};
    R.forEach(item => {
        if(item.code === 409) {
            os = removeFromRelation(os, item.uuid);
            if(!R.contains(item.conflict, os.meta.relations.HAS.uuids))
                os = addRelation(os, item.conflict);
        } else {
            os = removeFromRelation(os, item.uuid);
        }
    }, list);

    result.os = os;
    result.errors = R.reject(R.propEq('code', 409), list);
    return result;
};

const main = (db) => {
    const helper = new Helper(db);
    
    let ipEntity = '';
    let subGraph;
    rest.setToken(options.token);

    rest.getInstanceByPredicate('addr', options.ip)
        .then(ipAddress => {
            if(R.isEmpty(ipAddress)) {
                console.log('CREATING NEW Ipv4Address BASED ON: '+options.ip);
                ipEntity = helper.ipData(options.ip, true);
            } else {
                console.log('REUSING Ipv4Address BASED ON: '+options.ip);
                ipEntity = ipAddress;
            }

            ansibleLib.setup(options.ip)
                .then(ansibleResult => {
                    subGraph = helper.transform(ansibleResult, ipEntity);
                    let promises = [];
                    R.forEach(item => promises.push(rest.createInstance({entities: [item]})), subGraph.entities);
                    return Promise.all(promises);
                })
                .then(createdEntities => {
                    let output = resolvePersistErrors(subGraph.os, createdEntities);
                    if(R.length(output.errors) !== 0) {
                        console.log('There are failed creations: ');
                        console.log(output.errors);
                    }
                    return rest.createInstance({entities: [output.os]});
                })
                .then(createdOS => {
                    console.log(createdOS);
                    return rest.createInstance({entities: [subGraph.root]});
                })
                .then(finalResult => {
                    if(R.is(String, finalResult)) finalResult = JSON.parse(finalResult);
                    log(finalResult);
                    process.exit(0);
                });
        })
        .catch(ex => console.log(ex));
};

if(R.isNil(options.ip)) {
    console.log('An IP address is required');
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