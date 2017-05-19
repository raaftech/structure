"use strict";

const R = require('ramda');
const FuzzyMatching = require('fuzzy-matching');

class TypeDB {
    constructor(types) {
        this.types = types;
        this.fuzzyMatcher = new FuzzyMatching(R.pluck('name',types));
    }

    getTypes() {
        return this.types;
    }

    find(uuid) {
        const hit = R.pathEq(['meta','uuid'], uuid);
        return R.find(hit, this.types);
    }

    match(name) {
        const condition = R.ifElse(
                            R.pipe(R.prop('distance'), R.lte(0.5)),
                            R.prop('value'),
                            R.always(name)
                          );

        let m = this.fuzzyMatcher.get(name);
        let data = R.find(R.propEq('name', condition(m)), this.types);
        return R.defaultTo(null, data);
    }
}

module.exports = TypeDB;