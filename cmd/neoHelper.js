"use strict";

const R = require("ramda");

const neoUrl = R.defaultTo("http://localhost:7474", process.env.NEO4JURL);
const username = R.defaultTo("neo4j", process.env.STRUCTURE_NEO4J_USERNAME);
const password = R.defaultTo("", process.env.STRUCTURE_NEO4J_PASSWORD);
const auth = "Basic " + Buffer.from(username + ":" + password).toString("base64");

/*
  Sends a cypher query to the Neo4j Query API. Returns a Promise of a list
  of rows, with one property for each returned column.
  @param query The cypher query
*/
const cypher = (query) => {
  return fetch(neoUrl + "/db/neo4j/query/v2", {
    method: "POST",
    headers: {
      Accept: "application/json",
      Authorization: auth,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ statement: query }),
  })
    .then((response) => response.json())
    .then((json) => {
      if (json.errors) throw new Error(R.pluck("message", json.errors).join("; "));
      return R.map(R.zipObj(json.data.fields), json.data.values);
    });
};

module.exports = {
  cypher: cypher,
};
