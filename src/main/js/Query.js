// Copyright © 2008-2014 RAAF Technology bv

var eb = require('vertx/event_bus');

eb.registerHandler('org.openpcf.universal.query.run', function(queryString, responder) {

    var queryJsonObject = { 'query': queryString };

    // Put the query message on the eventbus
    eb.send(
        'org.openpcf.universal.neo4vertx.cypher.query',
        queryJsonObject,
        function(res) {
            res.query = queryString;
            responder(res);
        }
    );
});
