"use strict";

const { execFile } = require("node:child_process");
const { promisify } = require("node:util");
const fs = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");

exports.setup = async (ip, local) => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "structure-ansible-"));
  try {
    const args = [ip, "-i", ip + ",", "-m", "ansible.builtin.setup", "--tree", directory];
    if (local) args.push("-c", "local");
    else args.push("-c", "ssh", "-u", "root");
    await promisify(execFile)("ansible", args, { timeout: 50000 });
    const result = JSON.parse(await fs.readFile(path.join(directory, ip), "utf8"));
    if (result.failed || result.unreachable || !result.ansible_facts) {
      throw new Error("Ansible setup failed: " + JSON.stringify(result));
    }
    const facts = result.ansible_facts;
    return {
      distribution: facts.ansible_distribution,
      distribution_version: facts.ansible_distribution_version,
      fqdn: facts.ansible_fqdn,
      hostname: facts.ansible_hostname,
      nodename: facts.ansible_nodename,
      shell: facts.ansible_user_shell,
      service_manager: facts.ansible_service_mgr,
    };
  } finally {
    await fs.rm(directory, { recursive: true, force: true });
  }
};
