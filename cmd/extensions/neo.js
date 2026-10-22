"use strict";

const R = require('ramda');
const path = require('path');
const helper = require(path.join(__dirname, '..','helper'));
const util = require('util');
const treeify = require(path.join(__dirname, '..', 'treeify'));
const cypher = require(path.join(__dirname, '..', 'neoHelper')).cypher;

module.exports = function(vorpal, answerCache) {
    vorpal.command('neo query', 'Directly query the neo4j graph database')
        .action(function(args, done) {
            const self = this;
            let q = helper.generateQuestion('query', 'input', 'Cypher query: ', {default: answerCache.get('neo','query')});
            self.prompt(q, function(result) {
                cypher(result.query)
                    .then(queryResult => self.log(helper.stringify(queryResult)))
                    .catch(error => self.log(error))
                    .then(() => {
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
                cypher(cq).then(queryResult => {
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
                }).catch(error => self.log(error)).then(() => done());
            });
        });
};