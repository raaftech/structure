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
const createData = (uuid, desc, longName, shortName, name, version) => {
    return {
        _typeUuid: uuid,
        desc: desc,
        longName: longName,
        name: name,
        shortName: shortName,
        ver: version
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

const dnsData = (dns,ip) => {
    let x = {
        name: 'DnsName',
        desc: 'The hostname of the machine',
        dnsname: R.defaultTo('unknown-'+ip, dns)
    };
    return createInstanceData([x], '94d10031-5246-4233-b25d-b5f8ef9d0b41');
};

const osData = (dist, version,hostname,ip) => {
    hostname = R.defaultTo('unknown-'+ip, hostname);
    let c = getConcept(dist);
	if(R.isNil(c)) {
	    let generic = createData('f05e325e-1aee-4892-96e4-8bcd6e97c528','An operating system.',
                                 'OperatingSystem','operatingsystem', hostname, '');
        return createInstanceData([generic], '1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa');
    }

    let x = createData(c._typeUuid, c.desc, c.longName, c.shortName, hostname, version);
    return createInstanceData([x], '1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa');
};

const interpreterData = (interpreter) => {
    let fn = R.pipe(R.split('/'), R.last);
    let c = getConcept(fn(interpreter));
	if(R.isNil(c)) {
        let generic = createData('d047f415-734c-4bd3-92b9-d68d5521ce69','A command or programming language interpreter.',
                                 'Interpreter','interpreter', 'Interpreter', '');
        return createInstanceData([generic], '2741cc5a-eb45-4532-9a64-c715c07f93bc');
    }
    let x = createData(c._typeUuid, c.desc, c.longName, c.shortName, c.name, '');
    return createInstanceData([x], '2741cc5a-eb45-4532-9a64-c715c07f93bc');
};

const mgrData = (mgr) => {
    if(mgr === 'bsdinit') mgr = 'bsdrcinit';
    let c = getConcept(mgr);
	if(R.isNil(c)) {
        let generic = createData('c3008eec-b454-4ef6-bf7e-0bd92c13d3c3','An operating system initialization system.',
                                 'ServiceManager','servicemanager', 'ServiceManager', '');
        return createInstanceData([generic], '34bfe99f-1d62-48cb-a8a3-e913b69ef225');
	}
    let x = createData(c._typeUuid, c.desc, c.longName, c.shortName, c.name, '');
    return createInstanceData([x], '34bfe99f-1d62-48cb-a8a3-e913b69ef225');
};

const daemonData = (sshd, httpd) => {
    let ssh = createData('7db4a3ef-2d3c-4071-929b-e81505fbd5e2', 'An SSH server.',
                         'SshServer', 'sshserver', 'SshServer', 'unknown');
    let http = createData('b46cb520-caaf-4d26-b59d-58edca723b8d','HTTP daemon',
                          'HTTPD', httpd, 'HTTPD', 'unknown');

    let list = [];
    if(notNil(sshd) && sshd !== 'none') list.push(ssh);
    if(notNil(httpd) && httpd !== 'none') list.push(http);

    return createInstanceData(list, '54126ca8-3a5e-43b9-a501-98c8d158049d');
};

const miscData = () => {
    let result = [];
    result.push(createInstanceData([],'39e12352-8305-4b5d-af60-f1725ec458b8'));
    result.push(createInstanceData([],'e8e3c643-a520-4d63-9088-dd27086a1879'));
    return result;
};

const transform = (data, ip, hostname,sshd, httpd) => {
    if(notNil(data.error)) {
        let r = [];
        r.push(osData('Unknown', '',hostname,ip));
        r.push(interpreterData('unknown'));
        r.push(mgrData('unknown'));
        r.push(ipData(ip, false));
        r.push(dnsData(hostname,ip));
        r.push(daemonData(sshd, httpd));

        return createEventData(R.concat(r, miscData()));
    }

    let result = [];
    result.push(osData(data.distribution, data.distribution_version,hostname,ip));
    result.push(interpreterData(data.shell));
    result.push(mgrData(data.service_manager));
    result.push(ipData(ip, true));
    result.push(dnsData(data.nodename,ip));
    result.push(daemonData(sshd, httpd));

    return createEventData(R.concat(result, miscData()));
};


module.exports = {
    transform: transform
};