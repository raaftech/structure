var vorpal = require('vorpal')();
var R = require('ramda');
var util = require('util');
var helper = require('./helper');
var EventBus = require('vertx3-eventbus-client');
var neo = require('./extensions/neo');

var eventBusUrl = '';
var eb = null;
init();


function init() {
	eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
	eb = new EventBus(eventBusUrl);
	wrapper().then((res) => {
		console.log(res);
		scli();
	});
}

function wrapper() {
	return new Promise((resolve,reject) => {
		eb.onopen = () => {
			resolve(util.format('Connected to: %s', eventBusUrl));
		}
	});
}

function scli() {
	vorpal.history('structure-history');
	vorpal.use(neo);
	vorpal.delimiter('structure$').show();
}


function sendEvent(base, cmd, args) {
	var options = helper.createOptions(cmd,args);
	return new Promise((resolve,reject) => {
		eb.send(base, options, (error, result) => {
			if(error) reject(error);
			else resolve(result);
		});
	});
}



//commands
//TODO extract commands to extension sets

vorpal.command('disconnect', 'Disconnect from the EventBus')
    .action(function(args, done) {
        this.log(util.format('Closing connection to EventBus (%s)', eventBusUrl));
        eb.close();
        this.log('Connection closed.');
    });

vorpal.command('find <name>', 'Find a specified node in vertx')
	.option('-f, --field <fieldName>', 'Select a specific field from the resultset',
			helper.getCommonFields())
	.action(function(args, done) {
		const self = this;
		sendEvent(helper.getEventBusAddress(['repository', 'nodes']), 'findOneByName', {'name': args.name})
			.then((data) => {
			    var response = '';
				if(args.options.field) {
					response = helper.stringify(helper.getResultData(data)[args.options.field]);
				} else {
					response = helper.stringify(helper.getResultData(data));
				}

				self.log(response);
				done();
			});
	});


vorpal.command('dig <uuid>', 'Get the child elements for a node')
    .action(function(args, done) {
        const self = this;
        sendEvent(helper.getEventBusAddress(['node','type',args.uuid]),'getSubtypes',{})
            .then((data) => {
                var response = R.project(['name','uuid'], helper.getResultData(data));
                self.log(helper.stringify(response));
                done();
            });
    });


vorpal.command('show <uuid>', 'Show a node by its uuid.')
    .option('-f, --field <fieldName>', 'Select a specific field from the resultset',
        ['getPath', 'help','version','uuid'])
    .action(function(args, done) {
        const self = this;
        sendEvent(helper.getEventBusAddress(['node','type',args.uuid]), 'help', {})
            .then((data) => {
                var response = '';
                if(args.options.field) {
                    response = helper.stringify(data.body.result[args.options.field]);
                } else {
                    response = helper.stringify(data.body.result);
                }
                self.log(response);
                done();
            });
    });