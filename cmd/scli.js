var vorpal = require('vorpal')();
var R = require('ramda');
var util = require('util');
var helper = require('./helper');
var EventBus = require('vertx3-eventbus-client');
var clc = require('cli-color');
var sprintf = require('sprintf-js').sprintf;
var treeify = require('treeify');
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
		eb.onopen = () => resolve(clc.green.bold(util.format('Connected to: %s', eventBusUrl)));

		eb.onclose = (error) => resolve(clc.red.bold(util.format('Error while connecting to [%s]: %s', eventBusUrl, error.reason)));
	});
}

function scli() {
	vorpal.history('structure-history');
	vorpal.use(neo);
	vorpal.delimiter(clc.magenta.bold('structure$')).show();
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

function getChildren(uuid) {
    return new Promise((resolve, reject) => {
        sendEvent(helper.getEventBusAddress(['node','type',uuid]),'getSubtypes',{})
            .then((data) => {
                var response = R.project(['name','uuid'], helper.getResultData(data));
                resolve(response);
            });
    });
}

function toRoot(uuid, childNodes) {
    return new Promise((resolve, reject) => {
        sendEvent(helper.getEventBusAddress(['repository', 'nodes']), 'findOne', {'node': uuid})
            .then((parent) => {
                parent = helper.getResultData(parent);
                var tree = {};
                tree[parent.name] = childNodes;
                if(R.isNil(parent.isa)) {
                    resolve(tree);
                } else {
                    resolve(toRoot(parent.isa, tree));
                }
            });
    });
}


//commands
//TODO extract commands to extension sets

vorpal.command('disconnect', 'Disconnect from the EventBus')
    .action(function(args, done) {
        this.log(clc.red(util.format('Closing connection to EventBus (%s)', eventBusUrl)));
        eb.close();
        this.log(clc.red.bold('Connection closed.'));
    });

vorpal.command('find <name>', 'Find a specified node in vertx')
	.option('-f, --field <fieldName>', 'Select a specific field from the resultset',
			helper.getCommonFields())
	.option('-u, --uuid', 'Set search mode to uuid based search.')
	.option('-i, --instance', 'Set search mode to instance based search')
	.action(function(args, done) {
		const self = this;
		var act = args.options.uuid ? 'findOne' : 'findOneByName';
		var pargs = args.options.uuid ? {'node': args.name} : {'name': args.name};
		var addr = args.options.instance ? ['repository', 'instances'] : ['repository', 'types'];
		sendEvent(helper.getEventBusAddress(addr), act, pargs)
			.then((data) => {
				if(args.options.field) {
					var val = helper.stringify(helper.getResultData(data)[args.options.field]);
					self.log(sprintf('%-16s: %s', args.options.field, val));
					done();
				} else {
				    var databuf = helper.getResultData(data);
				    var keys = R.pipe(R.keys, R.prepend('ALL'))(databuf);
				    self.prompt(helper.generateQuestion('list', 'list', 'Choose a field', {choices: keys}), function(result) {
				        if(result.list === 'ALL') self.log(helper.stringify(databuf));
				        else {
				            var prop = clc.green.bold(result.list);
				            self.log(util.format('%s: %s',prop,helper.stringify(databuf[result.list])));
				        }
				        done();
				    });
				}
			});
	});

vorpal.command('instance')
    .option('-l, --last <quantity>', 'Fetch the last (quantity) created instances')
    .option('-f, --find <name>', 'Find a specific instance by name')
    .action(function(args, done) {
        const self = this;
        var act = args.options.last ? 'nLastCreated' : 'findOneByName';
        var pargs = args.options.last ? {'amount': args.options.last} : {'name': args.options.find};
        var addr = ['repository', 'instances'];
        sendEvent(helper.getEventBusAddress(addr), act, pargs)
            .then((data) => {
                var databuf = helper.getResultData(data);
                self.log(helper.stringify(databuf));
                done();
            });
    });

vorpal.command('dig <uuid>', 'Get the child elements for a node')
    .action(function(args, done) {
        const self = this;
        sendEvent(helper.getEventBusAddress(['node','type',args.uuid]),'getSubtypes',{})
            .then((data) => {
                var response = R.project(['name','uuid'], helper.getResultData(data));
                R.forEach((item) => {
                    self.log(sprintf('%-16s: %24s', item.name,item.uuid));
                }, response);
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
                if(args.options.field) {
                    self.log(helper.stringify(data.body.result[args.options.field]));
                    done();
                } else {
                    var databuf = data.body.result;
                    var keys = R.pipe(R.keys, R.prepend('ALL'))(databuf);
                    self.prompt(helper.generateQuestion('list', 'list', 'Choose a field', {choices: keys}), function(result) {
                        if(result.list === 'ALL') self.log(helper.stringify(databuf));
                        else {
                            var prop = clc.green.bold(result.list);
                            self.log(util.format('%s: %s',prop,helper.stringify(databuf[result.list])));
                        }
                        done();
                    });
                }
            });
    });


vorpal.command('tree <name>', 'Show a relation tree for the requested node')
    .option('-u, --uuid', 'Make the search go by uuid instead of name')
    .action(function(args, done) {
        const self = this;
        var act = args.options.uuid ? 'findOne' : 'findOneByName';
        var pargs = args.options.uuid ? {'node': args.name} : {'name': args.name};
        sendEvent(helper.getEventBusAddress(['repository', 'types']), act, pargs)
            .then((data) => {
                var databuf = helper.getResultData(data);
                var tree = {};
                getChildren(databuf.uuid)
                    .then((children) => {
                        R.forEach((item) => {
                            tree[item.name] = item.uuid;
                        }, children);

                        toRoot(databuf.uuid, tree)
                            .then((parents) => {
                                self.log(treeify.asTree(parents,true));
                                done();
                            });
                    });
            });
    });