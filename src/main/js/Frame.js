var eb = require('vertx/event_bus');

var name = function(args, frame) {
    return (typeof(args.name) !== 'undefined') ? args.name: frame;
}

eb.registerHandler('frame.component.isa', function(args, responder) {

    var queryString = "MATCH (p:Component)<-[:ISA]-(c) WHERE p.name = '" + name(args, 'Component') + "' RETURN c";

    eb.send('query.run', queryString, function(res) {
        responder(res);
    });
});

eb.registerHandler('frame.component.has', function(args, responder) {

    var queryString = "MATCH (p:Component)-[:HAS]->(c) WHERE p.name = '" + name(args, 'Component') + "' RETURN c";

    eb.send('query.run', queryString, function(res) {
        responder(res);
    });
});

eb.registerHandler('frame.protocol', function(args, responder) {

    var queryString = "MATCH (p:Protocol)<-[:ISA]-(c) WHERE p.name = '" + name(args, 'Protocol') + "' RETURN c";

    eb.send('query.run', queryString, function(res) {
        responder(res);
    });
});

// Select a complete computer by name
// MATCH (p:Computer)-[:HAS*]->(c) WHERE p.name = 'MyComputer' RETURN p, c

// Select all computers
// MATCH (p{name: 'Computer'})<-[:ISA]-(c) RETURN c