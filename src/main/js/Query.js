// Copyright © 2008-2014 RAAF Technology bv

var eb = require('vertx/event_bus');

eb.registerHandler('org.openpcf.universal.query.run', function(args, responder) {

    var queryJsonObject = { 'query': args.queryString };

    // Put the query message on the eventbus
    eb.send(
        'org.openpcf.neo4vertx.cypher.query',
        queryJsonObject,
        function(res) {
            res.name = args.name;
            res.query = args.queryString;
            responder(res);
        }
    );
});
