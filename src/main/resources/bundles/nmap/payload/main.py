import argparse
import copy
import ipaddress
import json
from pathlib import Path
import shlex
import subprocess
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "core" / "payload"))
from structure_client import BusClient, Graph, relate


def scan(target, arguments):
    result = subprocess.run(
        ["nmap", *shlex.split(arguments), "-oX", "-", target],
        check=True, capture_output=True, text=True, timeout=50,
    )
    report = ET.fromstring(result.stdout)
    finished = report.find("runstats/finished")
    if finished is None or finished.get("exit") != "success":
        raise RuntimeError("nmap scan failed: " + result.stdout)
    hosts = []
    for host in report.findall("host"):
        status = host.find("status")
        address = host.find("address[@addrtype='ipv4']")
        if status is None or status.get("state") != "up" or address is None:
            continue
        hostname = host.find("hostnames/hostname")
        hosts.append((address.get("addr"), hostname.get("name") if hostname is not None else None))
    return hosts


def update_address(client, graph, address, reachable, hostname):
    before = copy.deepcopy(address)
    address["data"]["reachable"] = reachable
    if hostname:
        dns = graph.dns(hostname)
        client.persist([dns], [address])
        relate(address, "HAS", dns)
    if address != before:
        client.update(address)
    return address["meta"]["uuid"]


def main():
    parser = argparse.ArgumentParser(description="Discover a network or an existing IPv4 address")
    target = parser.add_mutually_exclusive_group(required=True)
    target.add_argument("-c", "--cidr")
    target.add_argument("-i", "--ip")
    parser.add_argument("-n", "--net", default="Ipv4Network")
    parser.add_argument("-a", "--args", default="-p22,80")
    options = parser.parse_args()
    if options.ip:
        ipaddress.IPv4Address(options.ip)
    else:
        ipaddress.IPv4Network(options.cidr, strict=False)
    with BusClient() as client:
        graph = Graph(client)
        if options.ip:
            address = client.find("addr", options.ip)
            if not address.get("meta"):
                raise ValueError("No IP address found for " + options.ip)
            hosts = dict(scan(options.ip, options.args))
            return [update_address(client, graph, address, options.ip in hosts, hosts.get(options.ip))]

        network = client.find("cidr", options.cidr)
        exists = bool(network.get("meta"))
        if not exists:
            network = graph.instance("Ipv4Network", {
                "name": options.net, "desc": "An IPv4 network", "cidr": options.cidr,
            })
            network["name"] = options.net
        before = copy.deepcopy(network)
        for ip, hostname in scan(options.cidr, options.args):
            address = client.find("addr", ip)
            if address.get("meta"):
                update_address(client, graph, address, True, hostname)
            else:
                address = graph.address(ip)
                if hostname:
                    dns = graph.dns(hostname)
                    client.persist([dns], [address])
                    relate(address, "HAS", dns)
                client.persist([address], [network])
            relate(network, "HAS", address)
        if not exists:
            return client.persist([network])
        if network != before:
            return client.update(network)
        return [network["meta"]["uuid"]]


if __name__ == "__main__":
    try:
        print(json.dumps(main()))
    except Exception as error:
        print(error, file=sys.stderr)
        if isinstance(error, subprocess.CalledProcessError):
            print(error.stderr or error.stdout, file=sys.stderr)
        sys.exit(1)
