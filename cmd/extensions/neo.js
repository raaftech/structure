"use strict";

const vorpal = require('vorpal')();
const neo4j = require('neo4j');
const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','helper'));
const util = require('util');
const treeify = require(path.join(__dirname, '..', 'treeify'));

const neoUrl = R.defaultTo('http://localhost:7474', process.env.NEO4JURL);
const db = new neo4j.GraphDatabase(neoUrl);

module.exports = function(vorpal, answerCache) {
    vorpal.command('neo query', 'Directly query the neo4j graph database')
        .action(function(args, done) {
            const self = this;
            let q = helper.generateQuestion('query', 'input', 'Cypher query: ', {default: answerCache.get('neo','query')});
            self.prompt(q, function(result) {
                db.cypher({
                    query: result.query
                }, function(error, queryResult) {
                    if(error) {
                        self.log(error);
                    } else {
                        self.log(helper.stringify(queryResult));
                    }
                    answerCache.set('neo','query', result.query);
                    answerCache.save();
                    done();
                });
            });
        });

    vorpal.command('neo group', 'Query structure instances by predicate')
        .action(function(args, done) {
            const self = this;
            let getProperty = (name,obj) => R.path(['n','properties',name],obj);
            let q = helper.generateQuestion('query', 'input', 'Grouping predicate: ', {default: answerCache.get('neo','predicate')});
            self.prompt(q, function(result) {
                let cq = util.format('match (n:Instance)-[:INSTANCE_OF|ISA*0..]->(c:Type {name:"%s"}) return n', result.query);
                db.cypher({
                    query: cq
                }, function(error, queryResult) {
                    if(error) {
                        self.log(error);
                    } else {
                        let foundNames = [];
                        let postName = 2;
                        let tree = {};
                        tree[result.query] = {};
                        R.forEach((item) => {
                            let n = R.defaultTo(getProperty("name",item), getProperty("data-name",item));
                            let d = R.defaultTo(getProperty("desc",item), getProperty("data-desc",item));
                            if(R.contains(n, foundNames)) {
                                n = n+"-"+postName;
                                postName++;
                            } else {
                                foundNames = R.append(n, foundNames);
                            }
                            tree[result.query][n] = d;
                        }, queryResult);
                        self.log(treeify.asTree(tree,true));

                        answerCache.set('neo','predicate', result.query);
                        answerCache.save();
                    }
                    done();
                });
            });
        });
};