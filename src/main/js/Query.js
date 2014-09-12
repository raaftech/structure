// Copyright © 2008-2014 RAAF Technology bv

var eb = require('vertx/event_bus');

eb.registerHandler('query.run', function(queryString, responder) {

    var queryJsonObject = { 'query': queryString };

    // Put the query message on the eventbus
    eb.send(
        'neo4j-graph.cypher.query',
        queryJsonObject,
        function(res) {
            res.query = queryString;
            responder(res);
        }
    );
});
