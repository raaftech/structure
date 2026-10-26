"use strict";

const R = require("ramda");

const settings = require("../../../settings");

/*
  Sends a cypher query to the Neo4j Query API. Returns a Promise of a list
  of rows, with one property for each returned column.
  @param query The cypher query
*/
const cypher = (query) => {
  const config = settings.config();
  const auth = "Basic " + Buffer.from(config["neo4j-user"] + ":" + config["neo4j-pass"]).toString("base64");
  return fetch(config["neo4j-http-uri"] + "/db/neo4j/query/v2", {
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
