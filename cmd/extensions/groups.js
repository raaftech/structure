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

/*
    Creates a view of a node as returned from a cypher query
    @param node A node received from a cypher query
*/
const nodeLens = (node) => {
    let mkView = R.pipe(
        R.assoc('name', node.children.properties.name),
        R.assoc('type', node.children.properties.type),
        R.assoc('desc', node.children.properties.desc),
        R.assoc('uuid', node.children.properties.uuid));
    let ret = {};
    ret[node.children.properties.name] = mkView({});
    return ret;
};

module.exports = function(vorpal, options) {
    let rest = options;

    vorpal.command('group create', 'Create a new Grouping')
        .action(function(args, done) {
            const self = this;
            let qs = [];
            qs.push(helper.generateQuestion('name', 'input', 'Group name: '));
            qs.push(helper.generateQuestion('desc', 'input', 'Group desc: '));

            let loggedIn = () => {
                prompt(self, qs)
                    .then(answers => rest.createGroup(answers.name, answers.desc))
                    .then(created => {
                        console.log(clc.green('Group create result: '));
                        console.log(created);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    vorpal.command('group add', 'Add an existing instance to a group hierarchy')
        .action(function(args, done) {
            const self = this;
            let allGroups;
            let selectedGroup;

            let loggedIn = () =>  {
                getAllGroups()
                    .then(groups => {
                        if(R.length(groups) <= 0) throw new Error('No groups found');
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
                        let graph = helper.entity2Graph(selectedGroup);
                        return graph.toList();
                    })
                    .then(entityList => rest.updateInstance({entities: entityList}))
                    .then(created => {
                        console.log(clc.green('Instance grouping result: '));
                        console.log(created);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    //Note that this command is not working yet, since structure refuses to update relations
    vorpal.command('group del', 'Remove and existing instance from the group')
        .action(function(args, done) {
            let self = this;
            let selectedGroup;
            let allGroups;
            let allChildren;
            let selectedChildUuid;

            let loggedIn = () => {
                getAllGroups()
                    .then(groups => {
                        if(R.length(groups) <= 0) throw new Error('No groups found');
                        allGroups = groups;
                        let q = helper.generateQuestion('groupName', 'list', 'Select container group: ',{choices: groups});
                        return prompt(self, q);
                    })
                    .then(answer => {
                        selectedGroup = R.find(R.propEq('name', answer.groupName), allGroups);
                        let query = util.format('match (n:Grouping {name: "%s"})-[GROUPS]->(children:Instance) return children',answer.groupName);
                        return cypher(query);
                    })
                    .then(children => {
                        if(R.length(children) <= 0) throw new Error('No child instances found');
                        let mapped = R.map(nodeLens, children);
                        let getFirstPropertyName = R.compose(R.head, R.keys);
                        allChildren = R.map(item => R.prop(getFirstPropertyName(item), item), mapped);
                        let names = (x) => R.map(item => {return util.format('%s : %s',item.name, item.uuid)}, x);
                        let q = helper.generateQuestion('item', 'list', 'Select instance to remove: ', {choices: names(allChildren)});
                        return prompt(self, q);
                    })
                    .then(answer => {
                        selectedChildUuid = R.compose(R.last, R.split(' : '))(answer.item);
                        return rest.getEntityByUuid(selectedGroup.uuid);
                    })
                    .then(entity => {
                        let removeGrouping = (ent, uuid) => {
                            let updateList = R.pipe(R.path(['meta','relations','GROUPS','uuids']), R.reject(R.equals(uuid)));
                            let glist = updateList(ent);
                            if(R.length(glist) === 0) ent = R.dissocPath(['meta','relations','GROUPS'], ent);
                            else ent = R.assocPath(['meta','relations','GROUPS','uuids'], glist, ent);
                            return ent;
                        };
                        entity = removeGrouping(entity, selectedChildUuid);
                        let graph = helper.entity2Graph(entity);
                        return rest.updateInstance({entities: graph.toList()});
                    })
                    .then(updated => {
                        console.log(updated);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    vorpal.command('group remove', 'Delete an existing group')
        .action(function(args, done) {
            let self = this;
            let allGroups;

            let loggedIn = () => {
                getAllGroups()
                    .then(groups => {
                        if(R.length(groups) <= 0) throw new Error('No groups found');
                        allGroups = groups;
                        let q = helper.generateQuestion('group', 'list', 'Select container group: ',{choices: groups});
                        return prompt(self, q);
                    })
                    .then(answer => {
                        let e = R.find(R.propEq('name', answer.group),allGroups);
                        return rest.removeGroup(e.uuid);
                    })
                    .then(result => {
                        console.log(result);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    vorpal.command('group view', 'View grouping details for a specific grouping')
        .action(function(args, done) {
            let self = this;
            let allGroups;


            getAllGroups()
                .then(groups => {
                    if(R.length(groups) <= 0) throw new Error('No groups found');
                    allGroups = groups;
                    let q = helper.generateQuestion('groupName', 'list', 'Select group to view:', {choices: groups});
                    return prompt(self, q);
                })
                .then(answer => {
                    let query = util.format('match (n:Grouping {name: "%s"})-[GROUPS]->(children:Instance) return children',answer.groupName);
                    return cypher(query);
                })
                .then(result => {
                    let mapped = R.map(nodeLens, result);
                    console.log(helper.treePrint(false, mapped));
                    done();
                })
                .catch(helper.genericCatchHandler(done));
        });
};