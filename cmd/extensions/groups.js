'use strict';

const R = require('ramda');
const util = require('util');
const path = require('path');
const clc = require('cli-color');
const helper = require(path.join(__dirname, '..','helper'));
const graphLib = require(path.join(__dirname, '..', 'graphLib'));
const cypher = require(path.join(__dirname, '..', 'neoHelper')).cypher;

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
    let toTupleList = data => {
        return {
            name: data.n.properties.name,
            uuid: data.n.properties.uuid
        };
    };

    return cypher('match (n:Group) return n').then(R.map(toTupleList));
};

/*
    Creates a view of a node as returned from a cypher query
    @param node A node received from a cypher query
*/
const nodeLens = (node) => {
    let mkView = R.pipe(
        R.assoc('name', node.children.properties.name),
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
                        self.log(clc.green('Group create result: '));
                        self.log(created);
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
                        selectedGroup = theGroup;
                        let q = helper.generateQuestion('uuid', 'input', 'Instance uuid: ');
                        return prompt(self, q);
                    })
                    .then(answer => {
                        return rest.getEntityByUuid(answer.uuid);
                    })
                    .then(instance => {
                        let pair = graphLib.relateEntity(selectedGroup, 'MEMBER_OF', 'incoming', instance);
                        return rest.updateInstance({entities: [R.head(pair)]});
                    })
                    .then(updateResult => {
                        self.log(clc.green('Instance grouping result: '));
                        self.log(updateResult);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    //Note that this command is not working yet, since structure refuses to update relations
    vorpal.command('group del', 'Remove an existing child instance from a group')
        .action(function(args, done) {
            const self = this;
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
                        let query = util.format('match (n:Group {name: "%s"})<-[:MEMBER_OF]-(children:Instance) return children',answer.groupName);
                        return cypher(query);
                    })
                    .then(children => {
                        if(R.length(children) <= 0) throw new Error('No child instances found');
                        let mapped = R.map(nodeLens, children);
                        let getFirstPropertyName = R.compose(R.head, R.keys);
                        allChildren = R.map(item => R.prop(getFirstPropertyName(item), item), mapped);
                        let names = (x) => R.map(item => {return util.format('%s : %s',item.name, item.uuid);}, x);
                        let q = helper.generateQuestion('item', 'list', 'Select instance to remove: ', {choices: names(allChildren)});
                        return prompt(self, q);
                    })
                    .then(answer => {
                        selectedChildUuid = R.compose(R.last, R.split(' : '))(answer.item);
                        return rest.removeFromGroup(selectedGroup.uuid, selectedChildUuid);
                    })
                    .then(updated => {
                        self.log(updated);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    vorpal.command('group remove', 'Delete an existing group')
        .action(function(args, done) {
            const self = this;
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
                        self.log(result);
                        done();
                    })
                    .catch(helper.genericCatchHandler(done));
            };

            helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    vorpal.command('group view', 'View grouping details for a specific grouping')
        .action(function(args, done) {
            const self = this;
            let allGroups;


            getAllGroups()
                .then(groups => {
                    if(R.length(groups) <= 0) throw new Error('No groups found');
                    allGroups = groups;
                    let q = helper.generateQuestion('groupName', 'list', 'Select group to view:', {choices: groups});
                    return prompt(self, q);
                })
                .then(answer => {
                    let query = util.format('match (n:Group {name: "%s"})<-[:MEMBER_OF]-(children:Instance) return children',answer.groupName);
                    return cypher(query);
                })
                .then(result => {
                    let mapped = R.map(nodeLens, result);
                    self.log(helper.treePrint(false, mapped));
                    done();
                })
                .catch(helper.genericCatchHandler(done));
        });

    vorpal.command('group rename', 'Rename an existing group')
        .action(function(args, done) {
            const self = this;
            let groupName = '';
            let newName = '';

            getAllGroups()
                .then(groups => {
                    if(R.length(groups) <= 0) throw new Error('No groups found');
                    let q = helper.generateQuestion('groupName', 'list', 'Select group to view:', {choices: groups});
                    return prompt(self, q);
                })
                .then(answer => {
                    groupName = answer.groupName;
                    let q = helper.generateQuestion('newName', 'input', 'New group name: ');
                    return prompt(self, q);
                })
                .then(answer => {
                    newName = answer.newName;
                    return rest.getInstanceByName(groupName);
                })
                .then(list => {
                    let instance = R.head(list);
                    instance.name = newName;
                    instance.data.name = newName;
                    return rest.updateInstance({entities: [instance]});
                })
                .then(result => {
                    self.log(result);
                    done();
                })
                .catch(helper.genericCatchHandler(done));
        });
};
