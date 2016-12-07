"use strict";

const R = require('ramda');
const fs = require('fs');
const path = require('path');

const bundleData = JSON.parse(fs.readFileSync(path.join(__dirname, 'output.json')));
const getConcept = (name) => R.find(R.propEq('shortName', R.toLower(name)), bundleData);
const notNil = R.compose(R.not, R.isNil);

const createInstanceData = (instances, templateUuid) => {
    return {
        data: { instances: instances },
        templateUuid: templateUuid
    };
};

const createEventData = (dataList) => {
    return {
        args: {
            instancedata: dataList
        },
        msgh: 'instantiateDiscoveredComputer'
    };
};

const ipData = (ip,reachable) => {
    let x = {
        addr: ip,
        desc: 'Reachable IPv4 address',
        name: 'Ipv4Address',
        reachable: reachable
    };
    return createInstanceData([x], '0e843d97-c975-42ca-813c-c91f42a7d3cc');
};

const dnsData = (dns) => {
    let x = {
        name: 'DnsName',
        desc: 'The hostname of the machine',
        dnsname: dns
    };
    return createInstanceData([x], '94d10031-5246-4233-b25d-b5f8ef9d0b41');
};

const osData = (dist, version) => {
    let c = getConcept(dist);
	if(R.isNil(c)) {
	    console.log('osData: %s concept is undefined', dist);
	    let generic = {
	        _typeUuid: 'f05e325e-1aee-4892-96e4-8bcd6e97c528',
            desc: 'An operating system.',
            longName: 'OperatingSystem',
            name: 'OperatingSystem',
            shortName: R.toLower('OperatingSystem'),
            ver: ''
        };
        return createInstanceData([generic], '1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa');
    }
    let x = {
        _typeUuid: c._typeUuid,
        desc: c.desc,
        longName: c.longName,
        name: c.name,
        shortName: c.shortName,
        ver: version
    };

    return createInstanceData([x], '1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa');
};

const interpreterData = (interpreter) => {
    let fn = R.pipe(R.split('/'), R.last);
    let c = getConcept(fn(interpreter));
	if(R.isNil(c)) {
	    console.log('interpreterData: %s concept is undefined', interpreter);
        let generic = {
            _typeUuid: 'd047f415-734c-4bd3-92b9-d68d5521ce69',
            desc: 'A command or programming language interpreter.',
            longName: 'Interpreter',
            name: 'Interpreter',
            shortName: R.toLower('Interpreter'),
            ver: ''
        };
        return createInstanceData([generic], '1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa');
    }
    let x = {
        _typeUuid: c._typeUuid,
        desc: c.desc,
        longName: c.longName,
        name: c.name,
        shortName: c.shortName,
        ver: ''
    };

    return createInstanceData([x], '2741cc5a-eb45-4532-9a64-c715c07f93bc');
};

const mgrData = (mgr) => {
    let c = getConcept(mgr);
	if(R.isNil(c)) {
	    console.log('mgrData: %s concept is undefined', mgr);
        let generic = {
            _typeUuid: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3',
            desc: 'An operating system initialization system.',
            longName: 'ServiceManager',
            name: 'ServiceManager',
            shortName: R.toLower('ServiceManager'),
            ver: ''
        };
        return createInstanceData([generic], '1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa');
	}
    let x = {
        _typeUuid: c._typeUuid,
        desc: c.desc,
        longName: c.longName,
        name: c.name,
        shortName: c.shortName,
        ver: ''
    };
    return createInstanceData([x], '34bfe99f-1d62-48cb-a8a3-e913b69ef225');
};

const miscData = () => {
    let result = [];
    result.push(createInstanceData([],'39e12352-8305-4b5d-af60-f1725ec458b8'));
    result.push(createInstanceData([],'54126ca8-3a5e-43b9-a501-98c8d158049d'));
    result.push(createInstanceData([],'e8e3c643-a520-4d63-9088-dd27086a1879'));
    return result;
};

const transform = (data, ip, hostname) => {
    if(notNil(data.error)) {
        let r = [];
        r.push(osData('Unknown', ''));
        r.push(interpreterData('unknown'));
        r.push(mgrData('unknown'));
        r.push(ipData(ip, false));
        r.push(dnsData(hostname));

        return createEventData(R.concat(r, miscData()));
    }

    let result = [];
    result.push(osData(data.distribution, data.distribution_version));
    result.push(interpreterData(data.shell));
    result.push(mgrData(data.service_manager));
    result.push(ipData(R.defaultTo(R.head(data.ipv4_addresses),data.default_ipv4.address), true));
    result.push(dnsData(data.nodename));

    return createEventData(R.concat(result, miscData()));
};


module.exports = {
    transform: transform
};
