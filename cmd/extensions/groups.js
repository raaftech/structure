'use strict';

const vorpal = require('vorpal')();
const neo4j = require('neo4j');
const R = require('ramda');
const util = require('util');
const path = require('path');
const clc = require('cli-color');
const helper = require(path.join(__dirname, '..','helper'));
const Structure = require(path.join(__dirname, '..', 'subgraph'));

const neoUrl = R.defaultTo('http://localhost:7474', process.env.NEO4JURL);
const db = new neo4j.GraphDatabase(neoUrl);

const genericAuthCommandHandler = R.curry((done, loggedInFn, token) => {
    let loggedOut = () => { console.log('Please login first.'); done()};
    let handler = R.ifElse(R.isNil,loggedOut, loggedInFn);
    handler(token);
});
const genericCatchHandler = R.curry((done, error) => {
    console.log(error);
    done();
});
const createInstance = (baseTypeEntity, instanceEntity) => {
    let rel = instanceEntity.relationCreate('INSTANCE_OF', 'outgoing');
    rel.addEntity(baseTypeEntity);
    let container = {};
    container[instanceEntity.meta.uuid] = instanceEntity;
    return container;
};

const cypher = (query) => {
    return new Promise((resolve, reject) => {
        db.cypher({query: query}, function(error, queryResult) {
            if(error) reject(error);
            else resolve(queryResult);
        });
    });
};

const prompt = (vorp, question) => {
    return new Promise((resolve, reject) => {
        vorp.prompt(question, function(result) {
            resolve(result);
        });
    });
};

const getAllGroups = () => {
    return new Promise((resolve, reject) => {
        let q = 'match (n {type: "Grouping"}) return n';
        db.cypher({query: q}, function(error, queryResult) {
            if(error) { reject(error) }
            else {
                let toTupleList = data => {
                    return {
                        name: data.n.properties.name,
                        uuid: data.n.properties.uuid
                    }
                };

                resolve(R.map(toTupleList, queryResult));
            }
        });
    });
};

const getChildLayer = (parentName) => {
    let q = util.format('match (n:Type)-[ISA]->(c {name: "%s"}) return n', parentName);
    return cypher(q)
            .then(result => {
                let toTupleList = data => {
                    return {
                        name: data.n.properties.name,
                        uuid: data.n.properties.uuid
                    }
                };
                return R.map(toTupleList, result);
            });
};

module.exports = function(vorpal, options) {
    let rest = options;

    vorpal.command('group create <name>', 'Create a new Grouping')
        .action(function(args, done) {
            let loggedIn = () => {
                rest.getEntityByUuid('5f1eb7c5-ec5e-4563-be6a-fc50774fc374')
                    .then(entity => {
                        let baseType = helper.json2Entity(false, entity);
                        entity = R.pipe(
                            R.assoc('type', 'Grouping'),
                            R.assoc('name', args.name),
                            R.dissocPath(['meta','checksum'])) (entity);

                        let instance = createInstance(baseType, helper.json2Entity(true, entity));
                        return new Structure.SubGraph(instance).toList();
                    })
                    .then(entityList => rest.createInstance({entities: entityList}))
                    .then(created => {
                        console.log(clc.green('Group create result: '));
                        console.log(created);
                        done();
                    })
                    .catch(genericCatchHandler(done));
            };

            genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    //FIXME should be in ACL
    vorpal.command('group instance', 'Add an existing instance to a group hierarchy')
        .action(function(args, done) {
            let self = this;
            let allGroups;
            let selectedGroup;

            getAllGroups()
                .then(groups => {
                    allGroups = groups;
                    let q = helper.generateQuestion('group', 'list', 'Select container group: ',{choices: groups});
                    return prompt(self, q);
                })
                .then(answer => {
                    let e = R.find(R.propEq('name', answer.group),allGroups);
                    return rest.getEntityByUuid(e.uuid);
                })
                .then(theGroup => {
                    selectedGroup = helper.json2Entity(false, theGroup);
                    console.log(selectedGroup);
                    let q = helper.generateQuestion('uuid', 'input', 'Instance uuid: ');
                    return prompt(self, q);
                })
                .then(answer => {
                    return rest.getEntityByUuid(answer.uuid);
                })
                .then(instance => {
                    let rel = selectedGroup.relationCreate('GROUPS', 'outgoing');
                    rel.addEntity(helper.json2Entity(false, instance));
                    let container = {};
                    container[selectedGroup.meta.uuid] = selectedGroup;
                    let graph = new Structure.SubGraph(container);
                    return graph.toList();
                })
                .then(entityList => rest.updateInstance({entities: entityList}))
                .then(created => {
                    console.log(clc.green('Instance grouping result: '));
                    console.log(created);
                    done();
                })
                .catch(genericCatchHandler(done));
        });

    vorpal.command('group delete <uuid>', '')
        .action(function(args, done) {
            done();
        });
};