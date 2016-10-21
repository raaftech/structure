var vorpal = require('vorpal')();
var neo4j = require('neo4j');
var R = require('ramda');
var helper = require('../helper');

var neoUrl = R.defaultTo('http://localhost:7474', process.env.NEO4JURL);
var db = new neo4j.GraphDatabase(neoUrl);

module.exports = function(vorpal, options) {
    vorpal.command('neo query', 'Directly query the neo4j graph database')
        .action(function(args, done) {
            const self = this;
            var q = helper.generateQuestion('query', 'input', 'Cypher query: ');
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
};