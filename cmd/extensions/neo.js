"use strict";

const vorpal = require('vorpal')();
const neo4j = require('neo4j');
const R = require('ramda');
const helper = require('../helper');
const util = require('util');
const treeify = require('treeify');

const neoUrl = R.defaultTo('http://localhost:7474', process.env.NEO4JURL);
const db = new neo4j.GraphDatabase(neoUrl);

//match (n:Instance)-[:INSTANCE_OF|:ISA*1..]->(c {name:"Linux"}) match (n)-[:HAS_COMPONENT]->(dnsname)-[:INSTANCE_OF]->(t {name:"DnsName"}) return dnsname.`kv-dnsname`
module.exports = function(vorpal, options) {
    vorpal.command('neo query', 'Directly query the neo4j graph database')
        .action(function(args, done) {
            const self = this;
            let q = helper.generateQuestion('query', 'input', 'Cypher query: ');
            self.prompt(q, function(result) {
                db.cypher({
                    query: result.query
                }, function(error, queryResult) {
                    if(error) {
                        self.log(error);
                    } else {
                        self.log(helper.stringify(queryResult));
                    }
                    done();
                });
            });
        });

    vorpal.command('neo group', 'Query structure instances by predicate')
        .action(function(args, done) {
            const self = this;
            let q = helper.generateQuestion('query', 'input', 'Grouping predicate: ');
            self.prompt(q, function(result) {
                let cq = util.format('match (n:Instance)-[:INSTANCE_OF|:ISA*1..]->(c {name:"%s"}) match (n)-[:HAS_COMPONENT]->(dnsname)-[:INSTANCE_OF]->(t {name:"DnsName"}) match (n)-[:HAS_COMPONENT]->(addr)-[:INSTANCE_OF]->(x {name:"Ipv4Address"}) return dnsname.`kv-dnsname`, addr.`kv-addr`',result.query);
                db.cypher({
                    query: cq
                }, function(error, queryResult) {
                    if(error) {
                        self.log(error);
                    } else {
                        let tree = {};
                        tree[result.query] = {};
                        R.forEach((item) => {
                            tree[result.query][item["dnsname.`kv-dnsname`"]] = item["addr.`kv-addr`"];
                        }, queryResult);
                        self.log(treeify.asTree(tree,true));
                    }
                    done();
                });
            });
        });
};