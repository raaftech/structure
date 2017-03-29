'use strict';

const vorpal = require('vorpal')();
const R = require('ramda');
const util = require('util');
const path = require('path');
const clc = require('cli-color');

module.exports = function(vorpal, options) {
    let rest = options;

    vorpal.command('structure status', 'Show structure status information')
        .action(function(args, done) {
            const self = this;
            console.log(options);
            //Functions for usage percentage calculation
            let div = R.flip(R.divide);
            let calcUsedPercentage = (used, free) => R.pipe(R.add, div(used), R.divide(1000), Math.round, div(10))(used,free);
            let printPercentage = R.ifElse(R.lt(80), (p) => util.format('(%s%)',clc.red.bold(p)), (p) => util.format('(%s%)',clc.red.green(p)));
            let percentageFn = R.pipe(calcUsedPercentage, printPercentage);

            rest.getStructureStatusInfo()
                .then(result => {
                    self.log(clc.green.bold('[JVM info]'));
                    self.log(util.format('  Version: %s', result.jvm.version));
                    self.log(util.format('  Java version: %s',result.jvm.java_version));
                    self.log(util.format('  Cpus: %s', result.jvm.available_processors));
                    self.log(util.format(clc.green('  [Memory]')));
                    self.log(util.format('    Used: %s %s\n    Free: %s',result.jvm.memory.used, percentageFn(result.jvm.memory.used, result.jvm.memory.free), result.jvm.memory.free));

                    self.log(util.format(clc.green.bold('[Graph info]')));
                    self.log(util.format('  Types: %s', result.graph.types));
                    self.log(util.format('  Components: %s', result.graph.components));
                    self.log(util.format('  Instruments: %s', result.graph.instruments));
                    self.log(util.format('  Groupings: %s', result.graph.groupings));
                    self.log(util.format('  Results: %s', result.graph.results));
                    self.log(util.format('  Configurations: %s', result.graph.configurations));
                    done();
                })
                .catch(ex => {self.log(ex); done();})
        });

    vorpal.command('structure init', 'Purge-Init structure server')
        .action(function(args, done) {
            const self = this;

            rest.purgeInit()
                .then(result => {
                    self.log(result);
                    done();})
                .catch(ex => {self.log(ex); done();});
        });
};
