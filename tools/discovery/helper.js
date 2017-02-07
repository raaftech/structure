"use strict";

const R = require('ramda');
const fs = require('fs');
const path = require('path');
const util = require('util');
const FuzzyMatching = require('fuzzy-matching');
const Entity = require('./entity');

const bundleData = JSON.parse(fs.readFileSync(path.join(__dirname, 'output.json')));
const initMatcher = (types) => {
    let concepts = R.map(R.prop('shortName'),types);
    return new FuzzyMatching(concepts);
};
const fuzzyMatcher = initMatcher(bundleData);

const getConcept = (name) => {
    if(name === 'unknown') return null;

    let match = fuzzyMatcher.get(name);
    const f = R.ifElse(
                R.pipe(R.prop('distance'), R.lte(0.5)),
                R.prop('value'),
                R.always(name)
              );
     //console.log('Requested: %s, Resolved: %s, Distance: %s',name, f(match), JSON.stringify(match));
     let data = R.find(R.propEq('shortName', f(match)), bundleData);
     return R.defaultTo(null, data);
};

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
        desc: util.format('Reachable IPv4 address [%s]', ip),
        name: 'Ipv4Address',
        reachable: reachable
    };
    return createInstanceData([x], '0e843d97-c975-42ca-813c-c91f42a7d3cc');
};

const dnsData = (dns,ip) => {
    let x = {
        name: 'DnsName',
        desc: R.defaultTo('unknown-'+ip, dns),
        dnsname: R.defaultTo('unknown-'+ip, dns)
    };
    return createInstanceData([x], '94d10031-5246-4233-b25d-b5f8ef9d0b41');
};

const osData = (dist, version,hostname,ip) => {
    hostname = R.defaultTo('unknown-'+ip, hostname);
    let c = getConcept(dist);

	if(R.isNil(c)) {
	    //TODO handle relations
	    return new Entity.Entity('Type', 'OperatingSystem', dist,
	                                    'f05e325e-1aee-4892-96e4-8bcd6e97c528',
	                                    {hostname: hostname,version: R.defaultTo('unknown', version)});
    }

    //TODO insert relation to ISA parent
    return new Entity.Entity('Type', c.name, dist, c._typeUuid,
                             {hostname: hostname, version: R.defaultTo('unknown', version)});
};

const interpreterData = (interpreter,ip) => {
    let fn = R.pipe(R.split('/'), R.last);
    let c = getConcept(fn(interpreter));
	if(R.isNil(c)) {
	    //TODO handle relations
        return new Entity.Entity('Type', 'Interpreter', R.defaultTo('A command or programming language interpreter.', interpreter),
                                 'd047f415-734c-4bd3-92b9-d68d5521ce69');
    }

    //TODO insert relation to ISA parent
    return new Entity.Entity('Type', c.name, interpreter, c._typeUuid);
};

const mgrData = (mgr,ip) => {
    let c = getConcept(mgr);
	if(R.isNil(c)) {
        let generic = createData('c3008eec-b454-4ef6-bf7e-0bd92c13d3c3',R.defaultTo('An operating system initialization system.',mgr),
                                 'ServiceManager','servicemanager', 'ServiceManager', '');
        return createInstanceData([generic], '34bfe99f-1d62-48cb-a8a3-e913b69ef225');
	}
    let x = createData(c._typeUuid, mgr, c.longName, c.shortName, c.name, '');
    return createInstanceData([x], '34bfe99f-1d62-48cb-a8a3-e913b69ef225');
};

const daemonData = (sshd, httpd,ip) => {
    let ssh = createData('7db4a3ef-2d3c-4071-929b-e81505fbd5e2', R.defaultTo('An SSH server.',sshd),
                         'SshServer', 'sshserver', 'SshServer', 'unknown');
    let http = createData('b46cb520-caaf-4d26-b59d-58edca723b8d',R.defaultTo('HTTP daemon',httpd),
                          'HTTPD', httpd, 'HTTPD', 'unknown');

    let list = [];
    if(notNil(httpd) && httpd !== 'none') list.push(http);

    if(notNil(sshd) && sshd !== 'none') list.push(ssh);

    return createInstanceData(list, '54126ca8-3a5e-43b9-a501-98c8d158049d');
};

const miscData = () => {
    let result = [];
    result.push(createInstanceData([],'39e12352-8305-4b5d-af60-f1725ec458b8'));
    result.push(createInstanceData([],'e8e3c643-a520-4d63-9088-dd27086a1879'));
    return result;
};

const transform2 = (data, ip, hostname, sshd, httpd) => {
    let parentMap = {
        os: 'f05e325e-1aee-4892-96e4-8bcd6e97c528',
        interpreter: 'd047f415-734c-4bd3-92b9-d68d5521ce69',
        manager: 'c3008eec-b454-4ef6-bf7e-0bd92c13d3c3',
        ssh: '7db4a3ef-2d3c-4071-929b-e81505fbd5e2',
        http: 'b46cb520-caaf-4d26-b59d-58edca723b8d'
    };

    //TODO create the combining instance

    let os = osData(data.distribution, data.distribution_version,hostname,ip);
    let interpreter = interpreterDataNEW(data.shell);
    let manager = mgrDataNEW(data.service_manager, ip);
    let ip = ipDataNEW(ip, true);
    let dns = dnsDataNEW(data.nodename, ip);
    let daemon = daemonData(sshd, httpd, ip);

    return [os,interpreter,manager,ip,dns,daemon];
};

const transform = (data, ip, hostname,sshd, httpd) => {
    if(notNil(data.error)) {
        let r = [];
        r.push(osData('unknown', '',hostname,ip));
        r.push(interpreterData('unknown',ip));
        r.push(mgrData('unknown',ip));
        r.push(ipData(ip, true));
        r.push(dnsData(hostname,ip));
        r.push(daemonData(sshd, httpd,ip));

        return createEventData(R.concat(r, miscData()));
    }

    let result = [];
    result.push(osData(data.distribution, data.distribution_version,hostname,ip));
    result.push(interpreterData(data.shell));
    result.push(mgrData(data.service_manager,ip));
    result.push(ipData(ip, true));
    result.push(dnsData(data.nodename,ip));
    result.push(daemonData(sshd, httpd, ip));

    return createEventData(R.concat(result, miscData()));
};

module.exports = {
    transform: transform2
};
