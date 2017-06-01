'use strict';

const R = require('ramda');
const Validator = require('json-schema-validator');
const util = require('util');
const fetch = require('node-fetch');

const endsWith = R.curry((ch, str) => str.endsWith(ch));
const isTypeError = (error) => error.message.startsWith('Invalid type:');
const getExpectedType = R.path(['params','expected']);
const notNil = R.compose(R.not, R.isNil);
const notEmpty = R.compose(R.not, R.isEmpty);
const notEmptyOrNil = x => R.and(notNil(x), notEmpty(x));

/*
    Recursive helper function to correct schema type errors only. Input for vorpal always returns a string
    representation of the entered data. To comply to the actual schema, this function tries to cast the input
    to the type requested by the schema.
    @param v An instance of the Validator class
    @param u The uri for the schema to validate against
    @param i The object instance to validate
    @returns The corrected instance
 */
const correctTypeErrors = (v, u, i) => {
    let result = v.validate(i, u);
    if(!result.valid) {
        if(isTypeError(result.error)) {
            let field = R.pipe(R.split('/'), R.filter(notEmptyOrNil), R.head) (result.error.dataPath);
            i[field] = cast(getExpectedType(result.error), i[field]);
            return correctTypeErrors(v,u,i);
        } else {
            return i;
        }
    } else {
        return i;
    }
};

class SchemaValidator {
    constructor(baseUri) {
        this.baseUri = R.ifElse(endsWith('/'), R.identity, x => x + '/') (baseUri);
    }

    validate(typeName, instance) {
        let uri = util.format('%s%s', this.baseUri, typeName);

        return new Promise((resolve, reject) => {
            Validator.simple(uri, function(error, validator) {
                if(error) reject(error);
                else {
                    instance = correctTypeErrors(validator, uri, instance);
                    let result = validator.validate(instance, uri);
                    //Function to run if validation resulted in valid=true
                    let truthy = R.pipe(R.prop('valid'), R.objOf('valid'));
                    //Function to run if validation resulted in valid=false
                    let falsey = R.pipe(R.prop('error'), R.flip(R.append)([]), R.project(['message', 'dataPath']), R.head, R.assoc('valid',false));
                    let output = R.ifElse(R.propEq('valid', true), truthy, falsey) (result);
                    output.instance = instance;
                    resolve(output);
                }
            });
        });
    }

    /*
        Fetches a schema from the structure host for a specific type
        @param typeName The name of the type to fetch a schema for.
     */
    getSchema(typeName) {
        let url = util.format('%s%s.schema.json',this.baseUri, typeName);
        return fetch(url).then(result => result.json());
    }
}

/*
    Simple/naive function to cast values from String to the requested type
    @param type The name of the type to cast to. Supported: number, integer, boolean, object, array
    @returns The value, cast to the requested type
 */
function cast(type, value) {
    const strToBoolean = R.cond([
        [R.equals('true'), R.T],
        [R.equals('false'), R.F],
        [R.equals('1'), R.T],
        [R.equals('0'), R.F],
        [R.T, R.F]
    ]);

    const predicate = R.cond([
        [R.equals('number'), x => Number(value)],
        [R.equals('integer'), x => parseInt(value)],
        [R.equals('boolean'), x => strToBoolean(value)],
        [R.equals('object'), R.tryCatch(x => JSON.parse(value), x => {return {};})],
        [R.equals('array'), x => JSON.parse(value)],
        [R.T, x => value]
    ]);

    return predicate(type);
}

module.exports = SchemaValidator;