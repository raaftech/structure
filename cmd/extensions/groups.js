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

/*
    Wrapper function to handle calls to endpoints that have an ACL set.
    @param done The done() function from vorpal
    @param loggedInFn The function to run if logged in
    @param token The token for the REST service
 */
const genericAuthCommandHandler = R.curry((done, loggedInFn, token) => {
    let loggedOut = () => { console.log('Please login first.'); done()};
    let handler = R.ifElse(R.isNil,loggedOut, loggedInFn);
    handler(token);
});
/*
    Wrapper function to generically handle promise catch entries
    @param done The done() function from vorpal
    @param error The error object caught in the catch block
 */
const genericCatchHandler = R.curry((done, error) => {
    console.log(error);
    done();
});
/*
    Factory function for creating an instance based on a type
    @param baseTypeEntity The Entity object to base the instance on
    @param instanceEntity The instance object to link to the baseTypeEntity
    @returns A SubGraph compatible object representation of the new instance
 */
const createInstance = (baseTypeEntity, instanceEntity) => {
    let rel = instanceEntity.relationCreate('INSTANCE_OF', 'outgoing');
    rel.addEntity(baseTypeEntity);
    let container = {};
    container[instanceEntity.meta.uuid] = instanceEntity;
    return container;
};
/*
    Wrapper function that makes direct cypher queries to neo4j possible in a
    Promise based way
    @param query The cypher query
 */
const cypher = (query) => {
    return new Promise((resolve, reject) => {
        db.cypher({query: query}, function(error, queryResult) {
            if(error) reject(error);
            else resolve(queryResult);
        });
    });
};
/*
    Wrapper function for vorpal.prompt, Promise based.
    @param vorp The vorpal instance
    @param question The question or list of questions vorpal should ask the user
 */
const prompt = (vorp, question) => {
    return new Promise((resolve, reject) => {
        vorp.prompt(question, function(result) {
            resolve(result);
        });
    });
};
/*
    Fetches all known Grouping instances from structure
 */
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
/*
    Fetches all Type nodes that are children of the requested parent
    @param parentName The name of the parent type.
    @returns A list of Entities
 */
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

    //TODO add description question
    vorpal.command('group create <name>', 'Create a new Grouping')
        .action(function(args, done) {
            let loggedIn = () => {
                //FIXME fetch true Grouping type from structure
                rest.getEntityByUuid('5f1eb7c5-ec5e-4563-be6a-fc50774fc374')
                    .then(entity => {
                        let baseType = helper.json2Entity(false, entity);
                        entity = R.pipe(
                            R.assoc('type', 'Grouping'),
                            R.assoc('name', args.name),
                            R.dissocPath(['meta','checksum']),
                            R.dissocPath(['meta','relations'])) (entity);

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

    vorpal.command('group delete <uuid>', 'Delete an existing group')
        .action(function(args, done) {
            console.log('Not implemented yet');
            done();
        });

    vorpal.command('group view', 'View grouping details for a specific grouping')
        .action(function(args, done) {
            let self = this;
            let allGroups;
            /*
                Creates a view of a node as returned from a cypher query
                @param node A node received from a cypher query
             */
            let nodeLens = (node) => {
                let mkView = R.pipe(
                    R.assoc('name', node.children.properties.name),
                    R.assoc('type', node.children.properties.type),
                    R.assoc('desc', node.children.properties.desc),
                    R.assoc('uuid', node.children.properties.uuid));
                let ret = {};
                ret[node.children.properties.name] = mkView({});
                return ret;
            };

            getAllGroups()
                .then(groups => {
                    allGroups = groups;
                    let q = helper.generateQuestion('groupName', 'list', 'Select group to view:', {choices: groups});
                    return prompt(self, q);
                })
                .then(answer => {
                    let group = R.find(R.propEq('name', answer.groupName), allGroups);
                    let query = util.format('match (n:Grouping {name: "%s"})-[GROUPS]->(children:Instance) return children',answer.groupName);
                    return cypher(query);
                })
                .then(result => {
                    let mapped = R.map(nodeLens, result);
                    console.log(helper.treePrint(false, mapped));
                    done();
                })
                .catch(genericCatchHandler(done));
        });
};