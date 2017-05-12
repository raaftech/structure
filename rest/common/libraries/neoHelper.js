'use strict';

const R = require('ramda');
const neo4j = require('neo4j');

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

module.exports = {
    cypher: cypher
};