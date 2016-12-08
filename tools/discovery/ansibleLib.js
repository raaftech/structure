"use strict";

const Ansible = require('node-ansible');
const R = require('ramda');

const createErrorMessage = (error) => {return {error: error};};
const sanitize = (searchString, str) => {
    let i = R.indexOf(searchString, str) + R.length(searchString);
    return R.drop(i, str);
};
const createOutput = (data) => {
    const output = {};

    let d = R.prop('ansible_facts', data);

    output.ipv4_addresses = d.ansible_all_ipv4_addresses;
    output.ipv6_addresses = d.ansible_all_ipv6_addresses;
    output.architecture = d.ansible_architecture;
    output.default_ipv4 = d.ansible_default_ipv4;
    output.distribution = d.ansible_distribution;
    output.distribution_version = d.ansible_distribution_version;
    output.hostname = d.ansible_hostname;
    output.fqdn = d.ansible_fqdn;
    output.nodename = d.ansible_nodename;
    output.os_family = d.ansible_os_family;
    output.service_manager = d.ansible_service_mgr;
    output.system = d.ansible_system;
    output.shell = d.ansible_user_shell;

    return output;
};

const setupEnv = () => {
	process.env.ANSIBLE_HOST_KEY_CHECKING = false;
	process.env.ANSIBLE_PYTHON_INTERPRETER = "/usr/bin/python,/usr/local/bin/python2.7";
};

const setupScan = (ip) => {
    return new Promise((resolve, reject) => {
        setupEnv();
        const ansibleInit = new Ansible.AdHoc();
        if(ip !== 'localhost' && ip !== '127.0.0.1')
            ansibleInit.inventory(ip+',');
        const ansible = ansibleInit.module('setup')
            .user('root')
            .hosts(ip);

        ansible.exec()
            .then((result) => {
                //console.log(result);
                let json = JSON.parse(sanitize('SUCCESS => ', result.output));
                resolve(createOutput(json));
            })
            .catch((ex) => {
            //console.log(ex);
                resolve(createErrorMessage(ex));
            });
    });
};

const shellExec = (ip,args) => {
    return new Promise((resolve, reject) => {
        setupEnv();
        const ansibleInit = new Ansible.AdHoc();
        if(ip !== 'localhost' && ip !== '127.0.0.1')
            ansibleInit.inventory(ip+',');
        const ansible = ansibleInit.module('shell')
            .user('root')
            .hosts(ip)
            .args(args);

        ansible.exec()
            .then((result) => resolve(result))
            .catch((ex) => reject(createErrorMessage(ex)));
    });
};

module.exports = {
    setup: setupScan,
    shell: shellExec
};
