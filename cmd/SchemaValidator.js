'use strict';

const R = require('ramda');
const Validator = require('json-schema-validator');
const util = require('util');

const endsWith = R.curry((ch, str) => str.endsWith(ch));
const isTypeError = (error) => error.message.startsWith('Invalid type:');
const getExpectedType = R.path(['params','expected']);
const notNil = R.compose(R.not, R.isNil);
const notEmpty = R.compose(R.not, R.isEmpty);
const notEmptyOrNil = x => R.and(notNil(x), notEmpty(x));

const correctTypeErrors = (v, u, i) => {
    while(!v.validate(i, u).valid) {
        let result = v.validate(i, u);
        if(isTypeError(result.error)) {
            let field = R.pipe(R.split('/'), R.filter(notEmptyOrNil), R.head) (result.error.dataPath);
            let value = cast(getExpectedType(result.error), i[field]);
            i[field] = value;
            return correctTypeErrors(v,u,i);
        } else {
            return i;
        }
    }

    return i;
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
                    let truthy = R.pipe(R.prop('valid'), R.objOf('valid'));
                    let falsey = R.pipe(R.prop('error'), R.flip(R.append)([]), R.project(['message', 'dataPath']), R.head, R.assoc('valid',false));
                    let output = R.ifElse(R.propEq('valid', true), truthy, falsey) (result);
                    output['instance'] = instance;
                    resolve(output);
                }
            });
        });
    }
}

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
        [R.equals('object'), R.tryCatch(x => JSON.parse(value), x => {return {}})],
        [R.equals('array'), x => JSON.parse(value)],
        [R.T, x => value]
    ]);

    return predicate(type);
}

module.exports = SchemaValidator;