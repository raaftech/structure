from string import Template
import csv

templ = Template("""
{
    "args": {
        "instancedata": [
            {
                "data": {
                    "instances": [
                        {
                            "_typeUuid": "${os_uuid}",
                            "desc": "${os_desc}",
                            "longName": "${os_longname}",
                            "name": "${os_name}",
                            "shortName": "${os_shortname}",
                            "ver": "${os_ver}"
                        }
                    ]
                },
                "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
            },
            {
                "data": {
                    "instances": [
                        {
                            "_typeUuid": "${interpreter_uuid}",
                            "desc": "${interpreter_desc}",
                            "longName": "${interpreter_longname}",
                            "name": "${interpreter_name}",
                            "shortName": "${interpreter_shortname}",
                            "ver": "${interpreter_ver}"
                        }
                    ]
                },
                "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
            },
            {
                "data": {
                    "instances": [
                        {
                            "desc": "Server",
                            "longName": "Server",
                            "name": "Server",
                            "shortName": "Server",
                            "ver": "Server"
                        }
                    ]
                },
                "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
            },
            {
                "data": {
                    "instances": [
                        {
                            "desc": "Client",
                            "longName": "Client",
                            "name": "Client",
                            "shortName": "Client",
                            "ver": "Client"
                        }
                    ]
                },
                "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
            },
            {
                "data": {
                    "instances": [
                        {
                            "addr": "${ipv4_addr}",
                            "desc": "Reachable IPv4 address",
                            "name": "Ipv4Address",
                            "reachable": true
                        }
                    ]
                },
                "templateNodeUuid": "0e843d97-c975-42ca-813c-c91f42a7d3cc"
            },
            {
                "data": {
                    "instances": [
                        {
                            "name": "DnsName",
                            "desc": "The hostname of the machine.",
                            "dnsname": "${hostname}"
                        }
                    ]
                },
                "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
            },
            {
                "data": {
                    "instances": [
                        {
                            "_typeUuid": "${servicemanager_uuid}",
                            "name": "ServiceManager",
                            "longName": "ServiceManager",
                            "shortName": "ServiceManager",
                            "desc": "ServiceManager",
                            "ver": "unknown"
                        }
                    ]
                },
                "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
            }
        ]
    },
    "msgh": "instantiate"
}""")

objs = []

with open('vm_list.csv') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        data = dict(
                os_uuid=row['os_uuid'],
                os_desc=row['os_desc'],
                os_longname=row['os_longname'],
                os_name=row['os_name'],
                os_shortname=row['os_shortname'],
                os_ver=row['os_ver'],
                interpreter_uuid=row['interpreter_uuid'],
                interpreter_desc=row['interpreter_desc'],
                interpreter_longname=row['interpreter_longname'],
                interpreter_name=row['interpreter_name'],
                interpreter_shortname=row['interpreter_shortname'],
                interpreter_ver=row['interpreter_ver'],
                ipv4_addr=row['ipv4_addr'],
                hostname=row['hostname'],
                servicemanager_uuid=row['servicemanager_uuid']
        )
        objs.append(templ.substitute(data))

print("var objs = [%s]" % ",".join(objs))
