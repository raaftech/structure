'use strict';

const R = require('ramda');
const path = require('path');
const SchemaValidator = require(path.join(__dirname, '..', 'libraries', 'SchemaValidator'));

module.exports = function(Schema) {
    const validator = new SchemaValidator(R.defaultTo('http://localhost:8080/schemas/', process.env.SCHEMAHOST));

    Schema.getSchema = function(typeName, callback) {
        validator.getSchema(typeName)
            .then(schema => {
                callback(null, schema);
            })
            .catch(ex => {
                callback(ex);
            });
    };

    Schema.remoteMethod('getSchema', {
        accepts: [{arg: 'typeName', type: 'string', required: true}],
        returns: {root: true, type: 'object'},
        http: {verb: 'get', path: '/:typeName'},
        description: 'Fetch the json schema for a type'
    });

    Schema.validate = function(typeName, instance, callback) {
        validator.validate(typeName, instance)
            .then(result => {
                callback(null, result);
            })
            .catch(ex => {
                callback(ex);
            });
    };

    Schema.remoteMethod('validate', {
        accepts: [
            {arg: 'typeName', type: 'string', required: true},
            {arg: 'instance', type: 'object', required: true}
        ],
        returns: {root: true, type: 'object'},
        http: {verb: 'post', path: '/validate/:typeName'},
        description: 'validate an object instance against its structure schema'
    });
};
