'use strict';

const R = require('ramda');
const fs = require('fs');

class SimpleCache {
    constructor() {
        this.cache = {
            query: {
                address: '',
                cmd: ''
            },
            neo: {
                query: ''
            },
            structure: {
                token: ''
            }
        };
    }

    get(section, field) {
        return R.path([section, field], this.cache);
    }

    set(section, field, value) {
        this.cache = R.assocPath([section, field], value, this.cache);
    }

    init() {
        let self = this;
        let load = R.tryCatch(
            file => JSON.parse(fs.readFileSync(file, {encoding: 'utf8'})),
            file => self.cache
        );
        this.cache = load('tui.cache');
    }

    save() {
        fs.writeFileSync('tui.cache', JSON.stringify(this.cache), {encoding: 'utf8'});
    }
}

module.exports = SimpleCache;