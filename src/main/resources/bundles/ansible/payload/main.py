import argparse
import copy
import difflib
import ipaddress
import json
from pathlib import Path
import shlex
import subprocess
import sys
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "core" / "payload"))
from structure_client import BusClient, Graph, log, relate


def setup(ip, local):
    with tempfile.TemporaryDirectory(prefix="structure-ansible-") as directory:
        command = ["ansible", ip, "-i", ip + ",", "-m", "ansible.builtin.setup", "--tree", directory]
        command += ["-c", "local"] if local else ["-c", "ssh", "-u", "root"]
        log(shlex.join(command))
        subprocess.run(command, check=True, capture_output=True, text=True, timeout=50)
        result = json.loads((Path(directory) / ip).read_text())
        if result.get("failed") or result.get("unreachable") or not result.get("ansible_facts"):
            raise RuntimeError("Ansible setup failed: " + json.dumps(result))
        return result["ansible_facts"]


def match(graph, name, base):
    candidates = {item["name"].casefold(): item["name"] for item in graph.types if graph.is_type(item, base)}
    matches = difflib.get_close_matches((name or "").casefold(), candidates, n=1, cutoff=0.5)
    return candidates[matches[0]] if matches else base


def interfaces(facts):
    """Each IPv4 address of the interfaces in the facts, with its network,
    except loopback addresses."""
    found = {}
    for fact in facts.values():
        if not isinstance(fact, dict) or "device" not in fact:
            continue
        for ipv4 in [fact.get("ipv4"), *fact.get("ipv4_secondaries", [])]:
            if isinstance(ipv4, dict) and ipv4.get("address") and ipv4.get("netmask"):
                interface = ipaddress.IPv4Interface(ipv4["address"] + "/" + ipv4["netmask"])
                if not interface.is_loopback:
                    found[str(interface.ip)] = str(interface.network)
    return sorted(found.items())


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
    log("Facts of " + ip + ": " + ", ".join(
        key + " " + str(facts.get("ansible_" + key))
        for key in ("distribution", "distribution_version", "fqdn", "user_shell", "service_mgr")
    ))
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
        # The graph gives the type of an instance by its name
        previous = next((item for item in previous_children
                         if graph.type(item["meta"]["relations"]["INSTANCE_OF"]["to"][0])["meta"]["uuid"] == child_type),
                        None)
        if previous:
            child["meta"]["uuid"] = previous["meta"]["uuid"]
        else:
            entities.append(child)
    for child in [address, *children]:
        relate(operating_system, "HAS", child)
    relate(operating_system, "MEMBER_OF", root)

    # The addresses of the interfaces, and the networks that have them
    known_networks, new_networks, links = {}, {}, []
    for interface_ip, cidr in interfaces(facts):
        other = address if interface_ip == ip else client.find("addr", interface_ip)
        notes = ["known address" if other.get("meta") else "new address"]
        if not other.get("meta"):
            other = graph.address(interface_ip)
            entities.append(other)
        relate(operating_system, "HAS", other)
        if cidr not in known_networks and cidr not in new_networks:
            network = client.find("cidr", cidr)
            if network.get("meta"):
                known_networks[cidr] = network["meta"]["uuid"]
            else:
                network = graph.instance("Ipv4Network", {"name": cidr, "desc": "An IPv4 network", "cidr": cidr})
                network["name"] = cidr
                new_networks[cidr] = network
        notes.append(("new network " if cidr in new_networks else "known network ") + cidr)
        log("Interface address " + interface_ip + ": " + ", ".join(notes))
        if cidr in new_networks:
            relate(new_networks[cidr], "HAS", other)
        else:
            links.append((known_networks[cidr], other))
    entities.extend(new_networks.values())

    log("Types: operating system " + selected + ", " + ", ".join(
        "new " + graph.type(child["meta"]["relations"]["INSTANCE_OF"]["to"][0])["name"] if child in entities
        else "known " + graph.type(child["meta"]["relations"]["INSTANCE_OF"]["to"][0])["name"]
        for child in children
    ))
    if not existing.get("meta"):
        log("New host " + root["name"] + " with the operating system " + selected)
        identifiers = client.persist([*entities, root, operating_system])
        for network, other in links:
            client.relate(network, "HAS", other["meta"]["uuid"])
        return identifiers

    before = copy.deepcopy(existing)
    client.persist(entities, [operating_system])
    for network, other in links:
        client.relate(network, "HAS", other["meta"]["uuid"])
    current = graph.type(existing["meta"]["relations"]["INSTANCE_OF"]["to"][0])
    log("Known host, with the operating system " + existing["name"] + " of type " + current["name"])
    if current["name"] != selected and graph.is_type(graph.type(selected), current["name"]):
        log("Specialized the operating system from " + current["name"] + " to " + selected)
        client.request("manager.entitymanager", "specialize", {
            "instance": existing["meta"]["uuid"], "isa": graph.type(selected)["meta"]["uuid"],
        })
        existing["meta"]["relations"]["INSTANCE_OF"]["to"] = [graph.type(selected)["meta"]["uuid"]]
        current = graph.type(selected)
    existing["data"].update(graph.software(current["name"], operating_system["data"]["ver"])["data"])
    # The node has the name of its type, for example Linux, as its data has
    existing["name"], existing["desc"] = existing["data"]["name"], existing["data"]["desc"]
    if existing != before:
        client.update(existing)
    for identifier in operating_system["meta"]["relations"]["HAS"]["to"]:
        client.relate(existing["meta"]["uuid"], "HAS", identifier)
    return [existing["meta"]["uuid"]]


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
