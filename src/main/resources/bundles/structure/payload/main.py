import argparse
import json
from pathlib import Path
import shutil
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "core" / "payload"))
from structure_client import BusClient, Graph, relate


def main():
    parser = argparse.ArgumentParser(description="Register the Structure host", add_help=False)
    parser.add_argument("--help", action="help")
    parser.add_argument("-h", "--hostname", required=True)
    options = parser.parse_args()
    with BusClient() as client:
        graph = Graph(client)
        root = graph.instance("ComponentGroup", {
            "name": options.hostname, "desc": "The Structure host",
        })
        root["name"] = options.hostname
        operating_system = graph.software("OperatingSystem", "unknown")
        entities = [graph.dns(options.hostname), graph.address("127.0.0.1"), graph.software("Structure", "unknown")]
        for command, name in (
            ("sh", "Sh"), ("python3", "Python3x"), ("python2", "Python2x"),
            ("nmap", "Nmap"), ("ansible", "Ansible"), ("ssh", "OpenSshClient"), ("ping", "Iputils"),
        ):
            if shutil.which(command):
                entities.append(graph.software(name, "unknown"))
        for entity in entities:
            relate(operating_system, "HAS", entity)
        relate(operating_system, "MEMBER_OF", root)
        return client.persist([*entities, root, operating_system])


if __name__ == "__main__":
    try:
        print(json.dumps(main()))
    except Exception as error:
        print(error, file=sys.stderr)
        sys.exit(1)
