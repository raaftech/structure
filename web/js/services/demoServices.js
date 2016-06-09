angular.module("structureApp")
    .factory("demoSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            createObjs: function(scope) {
                var objs = [
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "c19c8786-1ed0-4fc1-9ee9-7f0d49022d2c",
                                                "desc": "Red Hat Enterprise Linux",
                                                "longName": "Red Hat Enterprise Linux",
                                                "name": "Red Hat Enterprise Linux",
                                                "shortName": "rhel",
                                                "ver": "6"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.1.2(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.50",
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
                                                "dnsname": "champ"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608bfaf1-9edc-4609-b5cd-56d8aeff8174",
                                                "name": "SysvInit",
                                                "longName": "SysvInit",
                                                "shortName": "sysvinit",
                                                "desc": "The System V init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "db95db3d-fe10-448a-9f30-2a05bf06e699",
                                                "desc": "SuSE Linux Enterprise Server",
                                                "longName": "SuSE Linux Enterprise Server",
                                                "name": "SuSE Linux Enterprise Server",
                                                "shortName": "sles",
                                                "ver": "11 SP2"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "3.2.51(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.68",
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
                                                "dnsname": "sizzle"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608bfaf1-9edc-4609-b5cd-56d8aeff8174",
                                                "name": "SysvInit",
                                                "longName": "SysvInit",
                                                "shortName": "sysvinit",
                                                "desc": "The System V init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "46758eba-4463-4779-91d9-9095f9d87097",
                                                "desc": "Windows Server",
                                                "longName": "Windows Server",
                                                "name": "Windows Server",
                                                "shortName": "windowsserver",
                                                "ver": "2012"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "bc101145-dde2-4363-a7ed-7091d2ff2621",
                                                "desc": "A generic DOS shell.",
                                                "longName": "A generic DOS shell.",
                                                "name": "DosShell",
                                                "shortName": "dosshell",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "65af133b-1a2c-4faa-8d2a-a288eaa4f412",
                                                "desc": "A generic SMB/CIFS Server.",
                                                "longName": "Generic Samba Server",
                                                "name": "SmbServer",
                                                "shortName": "smbserver",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "b46cb520-caaf-4d26-b59d-58edca723b8d",
                                                "desc": "The Internet Information Services HTTP server.",
                                                "longName": "Internet Information Services HTTP Server",
                                                "name": "IISHTTP",
                                                "shortName": "w3svc",
                                                "ver": "8"
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
                                                "addr": "172.17.1.56",
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
                                                "dnsname": "lucas"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "21b82fbf-803c-4056-b914-502e2ff9952d",
                                                "name": "Services",
                                                "longName": "Services",
                                                "shortName": "services",
                                                "desc": "Microsoft Services",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "368ea969-197b-4e18-8a73-c6b6b26beb9f",
                                                "desc": "Debian",
                                                "longName": "Debian",
                                                "name": "Debian",
                                                "shortName": "debian",
                                                "ver": "7.x"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.2.37(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.51",
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
                                                "dnsname": "dagon"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608bfaf1-9edc-4609-b5cd-56d8aeff8174",
                                                "name": "SysvInit",
                                                "longName": "SysvInit",
                                                "shortName": "sysvinit",
                                                "desc": "The System V init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "11666f58-6150-4a65-abea-b8c5fbddfde4",
                                                "desc": "DragonFlyBSD",
                                                "longName": "DragonFlyBSD",
                                                "name": "DragonFlyBSD",
                                                "shortName": "dragonflybsd",
                                                "ver": "3.x"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608c9a23-a194-49b0-a986-c34bd52bf691",
                                                "desc": "The C Shell.",
                                                "longName": "The C Shell.",
                                                "name": "Csh",
                                                "shortName": "csh",
                                                "ver": "6.18.01 (Astron) 2012-02-14"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.52",
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
                                                "dnsname": "drake"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "b4c067de-e3cf-4b5b-90ca-588725d1b6c4",
                                                "name": "SysvInit",
                                                "longName": "SysvInit",
                                                "shortName": "sysvinit",
                                                "desc": "The System V init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "16934de7-609f-4068-a18e-7eb7452b5f01",
                                                "desc": "Windows",
                                                "longName": "Windows",
                                                "name": "Windows",
                                                "shortName": "windows",
                                                "ver": "8"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "bc101145-dde2-4363-a7ed-7091d2ff2621",
                                                "desc": "A generic DOS shell.",
                                                "longName": "A generic DOS shell.",
                                                "name": "DosShell",
                                                "shortName": "dosshell",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.53",
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
                                                "dnsname": "eight"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "21b82fbf-803c-4056-b914-502e2ff9952d",
                                                "name": "Services",
                                                "longName": "Services",
                                                "shortName": "services",
                                                "desc": "Microsoft Services",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "a3b0f390-7257-43cb-9b9d-1cef8a4092bf",
                                                "desc": "Windows",
                                                "longName": "Windows",
                                                "name": "Windows",
                                                "shortName": "windows",
                                                "ver": "7"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "bc101145-dde2-4363-a7ed-7091d2ff2621",
                                                "desc": "A generic DOS shell.",
                                                "longName": "A generic DOS shell.",
                                                "name": "DosShell",
                                                "shortName": "dosshell",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.66",
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
                                                "dnsname": "seven"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "21b82fbf-803c-4056-b914-502e2ff9952d",
                                                "name": "Services",
                                                "longName": "Services",
                                                "shortName": "services",
                                                "desc": "Microsoft Services",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "31a0496d-9cb4-4aab-b3ed-86ddb566c0f4",
                                                "desc": "Windows",
                                                "longName": "Windows",
                                                "name": "Windows",
                                                "shortName": "windows",
                                                "ver": "XP"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "bc101145-dde2-4363-a7ed-7091d2ff2621",
                                                "desc": "A generic DOS shell.",
                                                "longName": "A generic DOS shell.",
                                                "name": "DosShell",
                                                "shortName": "dosshell",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.64",
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
                                                "dnsname": "roger"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "21b82fbf-803c-4056-b914-502e2ff9952d",
                                                "name": "Services",
                                                "longName": "Services",
                                                "shortName": "services",
                                                "desc": "Microsoft Services",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "0cb2b9f2-3dfe-4183-8c0d-519a2b16e68a",
                                                "desc": "Windows Server",
                                                "longName": "Windows Server",
                                                "name": "Windows Server",
                                                "shortName": "windowsserver",
                                                "ver": "2008 R2"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "bc101145-dde2-4363-a7ed-7091d2ff2621",
                                                "desc": "A generic DOS shell.",
                                                "longName": "A generic DOS shell.",
                                                "name": "DosShell",
                                                "shortName": "dosshell",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.57",
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
                                                "dnsname": "marcus"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "21b82fbf-803c-4056-b914-502e2ff9952d",
                                                "name": "Services",
                                                "longName": "Services",
                                                "shortName": "services",
                                                "desc": "Microsoft Services",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "8eb5e304-7d31-4645-89e2-8c39fefc7278",
                                                "desc": "Windows Server",
                                                "longName": "Windows Server",
                                                "name": "Windows Server",
                                                "shortName": "windowsserver",
                                                "ver": "2003 R2"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "bc101145-dde2-4363-a7ed-7091d2ff2621",
                                                "desc": "A generic DOS shell.",
                                                "longName": "A generic DOS shell.",
                                                "name": "DosShell",
                                                "shortName": "dosshell",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.74",
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
                                                "dnsname": "wodan"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "21b82fbf-803c-4056-b914-502e2ff9952d",
                                                "name": "Services",
                                                "longName": "Services",
                                                "shortName": "services",
                                                "desc": "Microsoft Services",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "ca39cf3a-43b2-46b9-8440-cf350c730d29",
                                                "desc": "Fedora",
                                                "longName": "Fedora",
                                                "name": "Fedora",
                                                "shortName": "fedora",
                                                "ver": "20"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.2.51(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.55",
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
                                                "dnsname": "friday"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "0c189a0e-6b78-4177-832d-5d9b35b8a6af",
                                                "name": "Systemd",
                                                "longName": "Systemd",
                                                "shortName": "systemd",
                                                "desc": "Systemd is a suite of basic building blocks for a Linux system.",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "09ad6070-a602-4e00-ba9c-25a181f7f0df",
                                                "desc": "openSUSE",
                                                "longName": "openSUSE",
                                                "name": "openSUSE",
                                                "shortName": "opensuse",
                                                "ver": "13.1"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.2.47(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.61",
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
                                                "dnsname": "ogami"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "0c189a0e-6b78-4177-832d-5d9b35b8a6af",
                                                "name": "Systemd",
                                                "longName": "Systemd",
                                                "shortName": "systemd",
                                                "desc": "Systemd is a suite of basic building blocks for a Linux system.",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "d98635d0-a229-4755-88e2-30f891ce692d",
                                                "desc": "Ubuntu",
                                                "longName": "Ubuntu",
                                                "name": "Ubuntu",
                                                "shortName": "ubuntu",
                                                "ver": "10.04"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.1.5(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.71",
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
                                                "dnsname": "umaril"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "3aac3563-a493-4efa-a31b-e751ee7d0a6f",
                                                "name": "Upstart",
                                                "longName": "Upstart",
                                                "shortName": "upstart",
                                                "desc": "Upstart is an event-based replacement for the /sbin/init daemon which handles starting of tasks and services during boot, stopping them during shutdown and supervising them while the system is running. ",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "0cf76620-de1f-4e7b-b51b-74ecd980c253",
                                                "desc": "Ubuntu",
                                                "longName": "Ubuntu",
                                                "name": "Ubuntu",
                                                "shortName": "ubuntu",
                                                "ver": "12.04"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.2.25(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.72",
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
                                                "dnsname": "uncle"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "3aac3563-a493-4efa-a31b-e751ee7d0a6f",
                                                "name": "Upstart",
                                                "longName": "Upstart",
                                                "shortName": "upstart",
                                                "desc": "Upstart is an event-based replacement for the /sbin/init daemon which handles starting of tasks and services during boot, stopping them during shutdown and supervising them while the system is running. ",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "61a034bb-19e3-4d8f-bda9-35c90e874dc8",
                                                "desc": "Ubuntu",
                                                "longName": "Ubuntu",
                                                "name": "Ubuntu",
                                                "shortName": "ubuntu",
                                                "ver": "14.04"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.3.11(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7db4a3ef-2d3c-4071-929b-e81505fbd5e2",
                                                "desc": "An SSH server.",
                                                "longName": "SshServer",
                                                "name": "SshServer",
                                                "shortName": "sshserver",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "b46cb520-caaf-4d26-b59d-58edca723b8d",
                                                "desc": "The Apache HTTP daemon.",
                                                "longName": "ApacheHTTPD",
                                                "name": "ApacheHTTPD",
                                                "shortName": "apache2",
                                                "ver": "2.4.7-1ubuntu4.9"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.73",
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
                                                "dnsname": "usher"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "3aac3563-a493-4efa-a31b-e751ee7d0a6f",
                                                "name": "Upstart",
                                                "longName": "Upstart",
                                                "shortName": "upstart",
                                                "desc": "Upstart is an event-based replacement for the /sbin/init daemon which handles starting of tasks and services during boot, stopping them during shutdown and supervising them while the system is running. ",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "e144cfb5-664a-4542-9125-74da72cc0a8a",
                                                "desc": "FreeBSD",
                                                "longName": "FreeBSD",
                                                "name": "FreeBSD",
                                                "shortName": "freebsd",
                                                "ver": "10"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608c9a23-a194-49b0-a986-c34bd52bf691",
                                                "desc": "The C Shell.",
                                                "longName": "The C Shell.",
                                                "name": "Csh",
                                                "shortName": "csh",
                                                "ver": "6.18.01 (Astron) 2012-02-14 "
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.54",
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
                                                "dnsname": "feline"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "b4c067de-e3cf-4b5b-90ca-588725d1b6c4",
                                                "name": "BsdrcInit",
                                                "longName": "BsdrcInit",
                                                "shortName": "bsdrcinit",
                                                "desc": "The Berkeley init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "8ac7f26d-588d-44de-8792-26d6d429f9ff",
                                                "desc": "NetBSD",
                                                "longName": "NetBSD",
                                                "name": "NetBSD",
                                                "shortName": "netbsd",
                                                "ver": "6.x"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7c1c3278-6eb4-4be5-8b8d-94204bfd2223",
                                                "desc": "The Bourne Shell.",
                                                "longName": "The Bourne Shell.",
                                                "name": "Ash",
                                                "shortName": "sh",
                                                "ver": "3.1/4.0 ff"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.60",
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
                                                "dnsname": "nihon"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "b4c067de-e3cf-4b5b-90ca-588725d1b6c4",
                                                "name": "BsdrcInit",
                                                "longName": "BsdrcInit",
                                                "shortName": "bsdrcinit",
                                                "desc": "The Berkeley init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6330dd4e-92bc-4cab-b218-1c18d37e9742",
                                                "desc": "OpenBSD",
                                                "longName": "OpenBSD",
                                                "name": "OpenBSD",
                                                "shortName": "openbsd",
                                                "ver": "5.x"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "fd95e858-c788-4578-9223-122eceb2e6c6",
                                                "desc": "The Korn Shell.",
                                                "longName": "The Korn Shell.",
                                                "name": "Ksh",
                                                "shortName": "ksh",
                                                "ver": "@(#)PD KSH v5.2.14 99/07/13.2"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7db4a3ef-2d3c-4071-929b-e81505fbd5e2",
                                                "desc": "An SSH server.",
                                                "longName": "SshServer",
                                                "name": "SshServer",
                                                "shortName": "sshserver",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "b46cb520-caaf-4d26-b59d-58edca723b8d",
                                                "desc": "The OpenBSD http daemon.",
                                                "longName": "httpd",
                                                "name": "httpd",
                                                "shortName": "httpd",
                                                "ver": "5.7"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.62",
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
                                                "dnsname": "orient"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "b4c067de-e3cf-4b5b-90ca-588725d1b6c4",
                                                "name": "BsdrcInit",
                                                "longName": "BsdrcInit",
                                                "shortName": "bsdrcinit",
                                                "desc": "The Berkeley init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "3a81f815-a426-4269-bdd9-a7ea7527325c",
                                                "desc": "Solaris",
                                                "longName": "Solaris",
                                                "name": "Solaris",
                                                "shortName": "solaris",
                                                "ver": "11"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "fd95e858-c788-4578-9223-122eceb2e6c6",
                                                "desc": "The Korn Shell.",
                                                "longName": "The Korn Shell.",
                                                "name": "Ksh",
                                                "shortName": "ksh",
                                                "ver": "(AT&T Research) 93u 2011-02-08"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7db4a3ef-2d3c-4071-929b-e81505fbd5e2",
                                                "desc": "An SSH server.",
                                                "longName": "SshServer",
                                                "name": "SshServer",
                                                "shortName": "sshserver",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "b46cb520-caaf-4d26-b59d-58edca723b8d",
                                                "desc": "The Apache HTTP daemon.",
                                                "longName": "ApacheHTTPD",
                                                "name": "ApacheHTTPD",
                                                "shortName": "http",
                                                "ver": "2.2"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.69",
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
                                                "dnsname": "strider"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "300beaaf-5b4c-48b1-be5b-a5fe3fa547b2",
                                                "name": "Smf",
                                                "longName": "Smf",
                                                "shortName": "smf",
                                                "desc": "Service Management Facility",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "e10cd458-1348-488b-8cc4-01af672828ae",
                                                "desc": "Solaris",
                                                "longName": "Solaris",
                                                "name": "Solaris",
                                                "shortName": "solaris",
                                                "ver": "10"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7c1c3278-6eb4-4be5-8b8d-94204bfd2223",
                                                "desc": "The Bourne Shell.",
                                                "longName": "The Bourne Shell.",
                                                "name": "Sh",
                                                "shortName": "sh",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.67",
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
                                                "dnsname": "shuriken"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "300beaaf-5b4c-48b1-be5b-a5fe3fa547b2",
                                                "name": "Smf",
                                                "longName": "Smf",
                                                "shortName": "smf",
                                                "desc": "Service Management Facility",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "c19c8786-1ed0-4fc1-9ee9-7f0d49022d2c",
                                                "desc": "Red Hat Enterprise Linux",
                                                "longName": "Red Hat Enterprise Linux",
                                                "name": "Red Hat Enterprise Linux",
                                                "shortName": "rhel",
                                                "ver": "6"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.1.2(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.49",
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
                                                "dnsname": "thirty"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608bfaf1-9edc-4609-b5cd-56d8aeff8174",
                                                "name": "SysvInit",
                                                "longName": "SysvInit",
                                                "shortName": "sysvinit",
                                                "desc": "The System V init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "04202e27-f9ff-4ce0-8418-0d4aaecd6257",
                                                "desc": "Red Hat Enterprise Linux",
                                                "longName": "Red Hat Enterprise Linux",
                                                "name": "Red Hat Enterprise Linux",
                                                "shortName": "rhel",
                                                "ver": "5"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "3.2.25(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.63",
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
                                                "dnsname": "regan"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608bfaf1-9edc-4609-b5cd-56d8aeff8174",
                                                "name": "SysvInit",
                                                "longName": "SysvInit",
                                                "shortName": "sysvinit",
                                                "desc": "The System V init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "d4fd6992-f34c-473b-9e05-929d7179ff73",
                                                "desc": "Red Hat Enterprise Linux",
                                                "longName": "Red Hat Enterprise Linux",
                                                "name": "Red Hat Enterprise Linux",
                                                "shortName": "rhel",
                                                "ver": "7"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "4.2.46(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7db4a3ef-2d3c-4071-929b-e81505fbd5e2",
                                                "desc": "An SSH server.",
                                                "longName": "SshServer",
                                                "name": "SshServer",
                                                "shortName": "sshserver",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "b46cb520-caaf-4d26-b59d-58edca723b8d",
                                                "desc": "The Apache HTTP Server is a powerful, efficient, and extensible web server.",
                                                "longName": "Apache HTTP Server",
                                                "name": "ApacheHTTP",
                                                "shortName": "httpd",
                                                "ver": "2.4.6"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.65",
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
                                                "dnsname": "septim"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "0c189a0e-6b78-4177-832d-5d9b35b8a6af",
                                                "name": "Systemd",
                                                "longName": "Systemd",
                                                "shortName": "systemd",
                                                "desc": "Systemd is a suite of basic building blocks for a Linux system.",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "66ef29ff-374d-492f-99fe-e6573fcbacdf",
                                                "desc": "Mac OS X",
                                                "longName": "Mac OS X",
                                                "name": "Mac OS X",
                                                "shortName": "osx",
                                                "ver": "10.6"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7c1c3278-6eb4-4be5-8b8d-94204bfd2223",
                                                "desc": "The Bourne Shell.",
                                                "longName": "The Bourne Shell.",
                                                "name": "Sh",
                                                "shortName": "sh",
                                                "ver": "3.2.48(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.59",
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
                                                "dnsname": "nichimen"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7b0bfefb-2212-4c58-a7c5-46ff66f0166a",
                                                "name": "Launchd",
                                                "longName": "Launchd",
                                                "shortName": "launchd",
                                                "desc": "OSX launch daemon",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "66ef29ff-374d-492f-99fe-e6573fcbacdf",
                                                "desc": "Mac OS X",
                                                "longName": "Mac OS X",
                                                "name": "Mac OS X",
                                                "shortName": "osx",
                                                "ver": "10.8"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7c1c3278-6eb4-4be5-8b8d-94204bfd2223",
                                                "desc": "The Bourne Shell.",
                                                "longName": "The Bourne Shell.",
                                                "name": "Sh",
                                                "shortName": "sh",
                                                "ver": "3.2.53(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7db4a3ef-2d3c-4071-929b-e81505fbd5e2",
                                                "desc": "An SSH server.",
                                                "longName": "SshServer",
                                                "name": "SshServer",
                                                "shortName": "sshserver",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "b46cb520-caaf-4d26-b59d-58edca723b8d",
                                                "desc": "The Apache HTTP Server is a powerful, efficient, and extensible web server.",
                                                "longName": "/System/Library/LaunchDaemons/org.apache.httpd.plist",
                                                "name": "ApacheHTTP",
                                                "shortName": "org.apache.httpd",
                                                "ver": "unknown"
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
                                                "addr": "172.17.1.58",
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
                                                "dnsname": "mirai"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "7b0bfefb-2212-4c58-a7c5-46ff66f0166a",
                                                "name": "Launchd",
                                                "longName": "Launchd",
                                                "shortName": "launchd",
                                                "desc": "OSX launch daemon",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "3f1076ed-dbe7-4ca6-8138-5a909bef9ce8",
                                                "desc": "SuSE Linux Enterprise Server",
                                                "longName": "SuSE Linux Enterprise Server",
                                                "name": "SuSE Linux Enterprise Server",
                                                "shortName": "sles",
                                                "ver": "10 SP4"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "3.1.17(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "172.17.1.70",
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
                                                "dnsname": "suzan"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "608bfaf1-9edc-4609-b5cd-56d8aeff8174",
                                                "name": "SysvInit",
                                                "longName": "SysvInit",
                                                "shortName": "sysvinit",
                                                "desc": "The System V init manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    },
                    {
                        "args": {
                            "instancedata": [
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "a5613527-2f64-4481-83f2-80b0cb427e28",
                                                "desc": "A generic Linux distro",
                                                "longName": "Generic Linux",
                                                "name": "Generic Linux",
                                                "shortName": "genericlinux",
                                                "ver": "unknown"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "6d467b28-449f-498c-9ca5-8660f522897f",
                                                "desc": "The Bourne Again Shell.",
                                                "longName": "The Bourne Again Shell.",
                                                "name": "Bash",
                                                "shortName": "bash",
                                                "ver": "3.1.17(1)-release"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "2741cc5a-eb45-4532-9a64-c715c07f93bc"
                                },
                                {
                                    "data": {
                                        "instances": [

                                        ]
                                    },
                                    "templateNodeUuid": "54126ca8-3a5e-43b9-a501-98c8d158049d"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "9e977808-78fd-4eb6-95e0-ff7c7011caa5",
                                                "desc": "The OpenSSH client.",
                                                "longName": "OpenSshClient",
                                                "name": "OpenSshClient",
                                                "shortName": "ssh",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "6619adef-3bcb-4e32-bfec-d235a7bf3e29",
                                                "desc": "IP utilities.",
                                                "longName": "IpUtils",
                                                "name": "IpUtils",
                                                "shortName": "iputils",
                                                "ver": "unknown"
                                            },
                                            {
                                                "_typeUuid": "f513ddfc-e50b-49b4-9bd8-64afc61ff7a5",
                                                "desc": "A *nix smb toolchain.",
                                                "longName": "Samba",
                                                "name": "Samba",
                                                "shortName": "samba",
                                                "ver": "4.2.11"
                                            },
                                            {
                                                "_typeUuid": "2f207919-c852-4177-9b8f-e4a368026ca6",
                                                "desc": "A tool to execute commands on Windows",
                                                "longName": "Winexe",
                                                "name": "Winexe",
                                                "shortName": "winexe",
                                                "ver": "1.1"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "e8e3c643-a520-4d63-9088-dd27086a1879"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "addr": "127.0.0.1",
                                                "desc": "Reachable IPv4 address",
                                                "name": "Ipv4Address",
                                                "reachable": false
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
                                                "dnsname": "structure"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "94d10031-5246-4233-b25d-b5f8ef9d0b41"
                                },
                                {
                                    "data": {
                                        "instances": [
                                            {
                                                "_typeUuid": "c3008eec-b454-4ef6-bf7e-0bd92c13d3c3",
                                                "name": "Generic Service Manager",
                                                "longName": "Generic Service Manager",
                                                "shortName": "genericservicemanager",
                                                "desc": "A generic service manager",
                                                "ver": "1.0"
                                            }
                                        ]
                                    },
                                    "templateNodeUuid": "34bfe99f-1d62-48cb-a8a3-e913b69ef225"
                                }
                            ]
                        },
                        "msgh": "instantiate"
                    }];

                for (var i = 0; i < objs.length; i++) {
                    vertxEventBusService.send("com.raaftech.structure.node.template.1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa", objs[i], 25000)
                        .then(function(reply) {
                            //console.log(reply);
                            scope.returnInstance(reply.result.data);
                        })
                        .catch(function(err) {
                            //console.log(err);
                        });
                }
            }
        };
    });
