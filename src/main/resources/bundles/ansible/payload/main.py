import argparse
import copy
import difflib
import ipaddress
import json
from pathlib import Path
import subprocess
import sys
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "core" / "payload"))
from structure_client import BusClient, Graph, relate


def setup(ip, local):
    with tempfile.TemporaryDirectory(prefix="structure-ansible-") as directory:
        command = ["ansible", ip, "-i", ip + ",", "-m", "ansible.builtin.setup", "--tree", directory]
        command += ["-c", "local"] if local else ["-c", "ssh", "-u", "root"]
        subprocess.run(command, check=True, capture_output=True, text=True, timeout=50)
        result = json.loads((Path(directory) / ip).read_text())
        if result.get("failed") or result.get("unreachable") or not result.get("ansible_facts"):
            raise RuntimeError("Ansible setup failed: " + json.dumps(result))
        return result["ansible_facts"]


def match(graph, name, base):
    candidates = {item["name"].casefold(): item["name"] for item in graph.types if graph.is_type(item, base)}
    matches = difflib.get_close_matches((name or "").casefold(), candidates, n=1, cutoff=0.5)
    return candidates[matches[0]] if matches else base


def discover(client, graph, ip, self_id):
    address = client.find("addr", ip)
    known_address = bool(address.get("meta"))
    existing = client.request("repository.instances", "findOperatingSystem", {
        "node": address["meta"]["uuid"],
    }) if known_address else {}
    if not known_address:
        address = graph.address(ip)
    local = self_id in existing.get("meta", {}).get("relations", {}).get("MEMBER_OF", {}).get("to", [])
    facts = setup(ip, local)
    selected = match(graph, facts.get("ansible_distribution"), "OperatingSystem")
    operating_system = graph.software(selected, facts.get("ansible_distribution_version", "unknown"))
    root = graph.instance("ComponentGroup", {
        "name": facts.get("ansible_fqdn") or facts.get("ansible_hostname") or facts.get("ansible_nodename") or ip,
        "desc": "A discovered host",
    })
    root["name"] = root["data"]["name"]
    children = [
        graph.software(match(graph, (facts.get("ansible_user_shell") or "").split("/")[-1], "Interpreter")),
        graph.software(match(graph, facts.get("ansible_service_mgr"), "ServiceManager")),
        graph.software("IcmpServer"),
    ]
    if not local:
        children.append(graph.software("SshServer"))
    previous_children = [client.instance(identifier) for identifier in
                         existing.get("meta", {}).get("relations", {}).get("HAS", {}).get("to", [])]
    entities = [] if known_address else [address]
    for child in children:
        child_type = child["meta"]["relations"]["INSTANCE_OF"]["to"][0]
        previous = next((item for item in previous_children
                         if item["meta"]["relations"]["INSTANCE_OF"]["to"][0] == child_type), None)
        if previous:
            child["meta"]["uuid"] = previous["meta"]["uuid"]
        else:
            entities.append(child)
    for child in [address, *children]:
        relate(operating_system, "HAS", child)
    relate(operating_system, "MEMBER_OF", root)
    if not existing.get("meta"):
        return client.persist([*entities, root, operating_system])

    before = copy.deepcopy(existing)
    client.persist(entities, [operating_system])
    current = graph.type(existing["meta"]["relations"]["INSTANCE_OF"]["to"][0])
    if current["name"] != selected and graph.is_type(graph.type(selected), current["name"]):
        client.request("manager.entitymanager", "specialize", {
            "instance": existing["meta"]["uuid"], "isa": graph.type(selected)["meta"]["uuid"],
        })
        existing["meta"]["relations"]["INSTANCE_OF"]["to"] = [graph.type(selected)["meta"]["uuid"]]
        current = graph.type(selected)
    existing["data"].update(graph.software(current["name"], operating_system["data"]["ver"])["data"])
    for identifier in operating_system["meta"]["relations"]["HAS"]["to"]:
        relate(existing, "HAS", {"meta": {"uuid": identifier}})
    return client.update(existing) if existing != before else [existing["meta"]["uuid"]]


def main():
    parser = argparse.ArgumentParser(description="Discover an operating system with Ansible")
    parser.add_argument("-i", "--ip", required=True)
    parser.add_argument("--self-id", help="UUID of the Structure host group")
    options = parser.parse_args()
    ipaddress.IPv4Address(options.ip)
    with BusClient() as client:
        return discover(client, Graph(client), options.ip, options.self_id)


if __name__ == "__main__":
    try:
        print(json.dumps(main()))
    except Exception as error:
        print(error, file=sys.stderr)
        if isinstance(error, subprocess.CalledProcessError):
            print(error.stderr or error.stdout, file=sys.stderr)
        sys.exit(1)
