'use strict';

const vorpal = require('vorpal')();
const R = require('ramda');
const util = require('util');
const path = require('path');
const clc = require('cli-color');
const helper = require(path.join(__dirname, '..','helper'));
const Structure = require(path.join(__dirname, '..', 'subgraph'));

const genericAuthCommandHandler = R.curry((done, loggedInFn, token) => {
    let loggedOut = () => { console.log('Please login first.'); done()};
    let handler = R.ifElse(R.isNil,loggedOut, loggedInFn);
    handler(token);
});
const genericCatchHandler = R.curry((done, error) => {
    console.log(error);
    done();
});
const createInstance = (baseTypeEntity, instanceEntity) => {
    let rel = instanceEntity.relationCreate('INSTANCE_OF', 'outgoing');
    rel.addEntity(baseTypeEntity);
    let container = {};
    container[instanceEntity.meta.uuid] = instanceEntity;
    return container;
};

module.exports = function(vorpal, options) {
    let rest = options;

    vorpal.command('group create <name>', 'Create a new Grouping')
        .action(function(args, done) {
            let loggedIn = () => {
                rest.getEntityByUuid('5f1eb7c5-ec5e-4563-be6a-fc50774fc374')
                    .then(entity => {
                        let baseType = helper.json2Entity(false, entity);
                        entity = R.pipe(
                            R.assoc('type', 'Grouping'),
                            R.assoc('name', args.name),
                            R.dissocPath(['meta','checksum'])) (entity);

                        let instance = createInstance(baseType, helper.json2Entity(true, entity));
                        return new Structure.SubGraph(instance).toList();
                    })
                    .then(entityList => rest.createInstance({entities: entityList}))
                    .then(created => {
                        console.log(clc.green('Group create result: '));
                        console.log(created);
                        done();
                    })
                    .catch(genericCatchHandler(done));
            };

            genericAuthCommandHandler(done, loggedIn, rest.getToken());
        });

    vorpal.command('group instance', '')
        .action(function(args, done) {
            done();
        });

    vorpal.command('group delete <uuid>', '')
        .action(function(args, done) {
            done();
        });
};