import argparse
import json
from pathlib import Path
import shutil
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "core" / "payload"))
from structure_client import BusClient, Graph, log, relate


TOOLS = (
    ("sh", "Sh"), ("python3", "Python3x"), ("python2", "Python2x"),
    ("nmap", "Nmap"), ("ansible", "Ansible"), ("ssh", "OpenSshClient"), ("ping", "Iputils"),
)


def main():
    parser = argparse.ArgumentParser(description="Register the Structure host", add_help=False)
    parser.add_argument("--help", action="help")
    parser.add_argument("-h", "--hostname", required=True)
    parser.add_argument("-u", "--uuid", help="UUID of the Structure component, the origin of the run")
    options = parser.parse_args()
    with BusClient() as client:
        graph = Graph(client)
        log("Bootstrap of the Structure host " + options.hostname)
        root = graph.instance("ComponentGroup", {
            "name": options.hostname, "desc": "The Structure host",
        })
        root["name"] = options.hostname
        operating_system = graph.software("OperatingSystem", "unknown")
        # A DNS name belongs to an address, as in a network discovery
        dns = graph.dns(options.hostname)
        address = graph.address("127.0.0.1")
        relate(address, "HAS", dns)
        structure = graph.software("Structure", "unknown")
        if options.uuid:
            structure["meta"]["uuid"] = options.uuid
        entities = [address, structure]
        for command, name in TOOLS:
            path = shutil.which(command)
            if path:
                log("Found " + command + ": " + path)
                entities.append(graph.software(name, "unknown"))
            else:
                log("Did not find " + command)
        for entity in entities:
            relate(operating_system, "HAS", entity)
        relate(operating_system, "MEMBER_OF", root)
        identifiers = client.persist([dns, *entities, root, operating_system])
        log("Added the group " + options.hostname + " with an operating system, the address 127.0.0.1 with "
            "the DNS name " + options.hostname + ", Structure and " + str(len(entities) - 2) + " tools")
        return identifiers


if __name__ == "__main__":
    try:
        print(json.dumps(main()))
    except Exception as error:
        print(error, file=sys.stderr)
        sys.exit(1)
