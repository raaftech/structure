"use strict";

const R = require('ramda');
const fs = require('fs');
const path = require('path');

const bundleData = JSON.parse(fs.readFileSync(path.join(__dirname, 'output.json'));
const getConcept = (name) => R.find(R.propEq('shortName', R.toLower(name)), bundleData);

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
        msgh: 'instantiate'
    };
};

const ipData = (ip) => {
    let x = {
        addr: ip,
        desc: 'Reachable IPv4 address',
        name: 'Ipv4Address',
        reachable: true
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

const transform = (data) => {
    let result = [];
    result.push(osData(data.distribution, data.distribution_version));
    result.push(interpreterData(data.shell));
    result.push(mgrData(data.service_manager));
    result.push(ipData(R.defaultTo(R.head(data.all_ipv4_addresses),data.default_ipv4.address)));
    result.push(dnsData(data.nodename));

    return createEventData(R.concat(result, miscData()));
};


module.exports = {
    transform: transform
};
