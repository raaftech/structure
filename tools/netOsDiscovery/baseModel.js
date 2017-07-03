module.exports = {
    "ns": "aeon",
    "netName": "myname",
    "configName": "pConfig",
    "processName": "pName",
    "start": "recv cidr",
    "model": {
        "places": [
            {
                "id": "Start",
                "im": 1,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "about to get all types from structure",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "about to nmap scan",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "After scan",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "stop or continue",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "listening for status messages",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "status event received",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "about to specialize",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "specialize attempted",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "End",
                "im": 0,
                "em": 1,
                "capacity": 0
            },
            {
                "id": "force end",
                "im": 0,
                "em": 1,
                "capacity": 0
            },
            {
                "id": "determine OS category",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "About to deep detect",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "check detect result",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "detect ok kpi",
                "im": 0,
                "em": 0,
                "capacity": 0
            },
            {
                "id": "detect failed kpi",
                "im": 0,
                "em": 0,
                "capacity": 0
            }
        ],
        "transitions": [
            {
                "id": "recv cidr"
            },
            {
                "id": "Get all types"
            },
            {
                "id": "Nmap scan"
            },
            {
                "id": "CreateFilter"
            },
            {
                "id": "Status listener"
            },
            {
                "id": "Handle event"
            },
            {
                "id": "Specialize"
            },
            {
                "id": "Loop exit"
            },
            {
                "id": "Still receiving"
            },
            {
                "id": "Forcing to end"
            },
            {
                "id": "Guess by port"
            },
            {
                "id": "UnixLike create"
            },
            {
                "id": "Windows create"
            },
            {
                "id": "Generic create"
            },
            {
                "id": "Detect"
            },
            {
                "id": "Detect OK"
            },
            {
                "id": "ok pass"
            },
            {
                "id": "Detect failed"
            },
            {
                "id": "failed pass"
            }
        ],
        "arcs": [
            {
                "id": "e34d57b8-c532-4aaf-bbf1-9566e5c3eda0",
                "from": "Start",
                "to": "recv cidr",
                "weight": 1
            },
            {
                "id": "08e03592-b10c-4935-b526-eef59f973dd5",
                "from": "recv cidr",
                "to": "about to get all types from structure",
                "weight": 1
            },
            {
                "id": "46bc4b5e-0455-45eb-ae69-208819a16d23",
                "from": "about to get all types from structure",
                "to": "Get all types",
                "weight": 1
            },
            {
                "id": "7d3a7548-30bc-4bd8-b811-a0a422e3126d",
                "from": "Get all types",
                "to": "about to nmap scan",
                "weight": 1
            },
            {
                "id": "23ba7571-d246-4ead-bab3-359ae33c889c",
                "from": "about to nmap scan",
                "to": "Nmap scan",
                "weight": 1
            },
            {
                "id": "00003b42-fdd1-4476-9bfb-8e09d335f0d0",
                "from": "Nmap scan",
                "to": "After scan",
                "weight": 1
            },
            {
                "id": "6dae4c4c-eaee-4a07-a8cf-88331ff067ee",
                "from": "After scan",
                "to": "CreateFilter",
                "weight": 1
            },
            {
                "id": "03b61a39-6e84-4239-812a-629de01a90ad",
                "from": "CreateFilter",
                "to": "stop or continue",
                "weight": 1
            },
            {
                "id": "71314a9b-9b12-431c-a5ba-6f1a28841de1",
                "from": "recv cidr",
                "to": "listening for status messages",
                "weight": 1
            },
            {
                "id": "80dfc380-0427-42a9-9641-b315a257a3ef",
                "from": "listening for status messages",
                "to": "Status listener",
                "weight": 1
            },
            {
                "id": "7a78b5d7-83c5-464b-b1b9-a419d3df15cf",
                "from": "Status listener",
                "to": "status event received",
                "weight": 1
            },
            {
                "id": "15cc43e0-359a-4123-866d-b9767ac95605",
                "from": "status event received",
                "to": "Handle event",
                "weight": 1
            },
            {
                "id": "16a0c78b-1e0c-4bd8-b274-32d281d90db5",
                "from": "Handle event",
                "to": "about to specialize",
                "weight": 1
            },
            {
                "id": "6d2a433a-eb1f-4971-9aa6-ede36ef0c0b2",
                "from": "about to specialize",
                "to": "Specialize",
                "weight": 1
            },
            {
                "id": "a911f25f-eeeb-4189-9f1f-ec3f1d229de7",
                "from": "Specialize",
                "to": "specialize attempted",
                "weight": 1
            },
            {
                "id": "4eccd0e5-a710-4e55-bb90-d752d318a8be",
                "from": "specialize attempted",
                "to": "Loop exit",
                "weight": 1
            },
            {
                "id": "3c2ddaea-43fc-41b4-8dad-65502c16a268",
                "from": "Loop exit",
                "to": "End",
                "weight": 1
            },
            {
                "id": "424b601a-8c3e-4b4e-834f-6508bdb8eaa5",
                "from": "specialize attempted",
                "to": "Still receiving",
                "weight": 1
            },
            {
                "id": "f86ca3c0-2176-4807-9fc3-307e03584b4e",
                "from": "Still receiving",
                "to": "listening for status messages",
                "weight": 1
            },
            {
                "id": "cb245e18-7db0-46ec-a1ce-811a2172cbaa",
                "from": "stop or continue",
                "to": "Forcing to end",
                "weight": 1
            },
            {
                "id": "9cce4616-f05a-45f2-a9fd-4952dd3c7237",
                "from": "Forcing to end",
                "to": "force end",
                "weight": 1
            },
            {
                "id": "c7e0cc4b-b3ff-45a0-a036-6fbfce540623",
                "from": "stop or continue",
                "to": "Guess by port",
                "weight": 1
            },
            {
                "id": "6ed7cdec-f695-439f-b10d-70c389207ac0",
                "from": "Guess by port",
                "to": "determine OS category",
                "weight": 1
            },
            {
                "id": "65820a08-0428-45e1-9751-195001510ec5",
                "from": "determine OS category",
                "to": "UnixLike create",
                "weight": 1
            },
            {
                "id": "bedc9ae1-6bd5-4e02-9ff2-51a5372e4036",
                "from": "UnixLike create",
                "to": "About to deep detect",
                "weight": 1
            },
            {
                "id": "8884d002-a025-4c16-906a-e515d8754e09",
                "from": "determine OS category",
                "to": "Windows create",
                "weight": 1
            },
            {
                "id": "ecf9e721-97f4-4886-85a4-d8d2e560ed5e",
                "from": "Windows create",
                "to": "About to deep detect",
                "weight": 1
            },
            {
                "id": "a82741d9-62d1-4107-9073-7551bf44d0e3",
                "from": "determine OS category",
                "to": "Generic create",
                "weight": 1
            },
            {
                "id": "4df8ff49-23bb-4dec-a7d5-e05af6ecb054",
                "from": "Generic create",
                "to": "About to deep detect",
                "weight": 1
            },
            {
                "id": "56f62caf-d507-4186-9e83-53f7f7cc99c7",
                "from": "About to deep detect",
                "to": "Detect",
                "weight": 1
            },
            {
                "id": "f04d4359-7020-44b8-9d15-36d1691a42e9",
                "from": "Detect",
                "to": "check detect result",
                "weight": 1
            },
            {
                "id": "c148c2a9-7848-465c-8112-b952335e8d99",
                "from": "check detect result",
                "to": "Detect OK",
                "weight": 1
            },
            {
                "id": "2821e98f-6b14-41e8-9d04-1e81bfc04f6f",
                "from": "Detect OK",
                "to": "detect ok kpi",
                "weight": 1
            },
            {
                "id": "a84604e7-a7f6-4821-b741-006a29a73e33",
                "from": "detect ok kpi",
                "to": "ok pass",
                "weight": 1
            },
            {
                "id": "119b6e40-bee8-4d0f-a3ae-1e3470a0cff0",
                "from": "ok pass",
                "to": "stop or continue",
                "weight": 1
            },
            {
                "id": "15dd91ca-712b-4bbd-897d-5721ea8186af",
                "from": "check detect result",
                "to": "Detect failed",
                "weight": 1
            },
            {
                "id": "d08a532f-6b41-46e9-b7ff-2199888a4d93",
                "from": "Detect failed",
                "to": "detect failed kpi",
                "weight": 1
            },
            {
                "id": "6fbe4cde-a46f-4985-a11c-91baa420eae5",
                "from": "detect failed kpi",
                "to": "failed pass",
                "weight": 1
            },
            {
                "id": "5c74918d-355c-45d8-888c-d3ee5f0b01eb",
                "from": "failed pass",
                "to": "stop or continue",
                "weight": 1
            }
        ],
        "markings": [
            {
                "id": "Start",
                "tokens": 1
            },
            {
                "id": "about to get all types from structure",
                "tokens": 0
            },
            {
                "id": "about to nmap scan",
                "tokens": 0
            },
            {
                "id": "After scan",
                "tokens": 0
            },
            {
                "id": "stop or continue",
                "tokens": 0
            },
            {
                "id": "listening for status messages",
                "tokens": 0
            },
            {
                "id": "status event received",
                "tokens": 0
            },
            {
                "id": "about to specialize",
                "tokens": 0
            },
            {
                "id": "specialize attempted",
                "tokens": 0
            },
            {
                "id": "End",
                "tokens": 0
            },
            {
                "id": "force end",
                "tokens": 0
            },
            {
                "id": "determine OS category",
                "tokens": 0
            },
            {
                "id": "About to deep detect",
                "tokens": 0
            },
            {
                "id": "check detect result",
                "tokens": 0
            },
            {
                "id": "detect ok kpi",
                "tokens": 0
            },
            {
                "id": "detect failed kpi",
                "tokens": 0
            }
        ],
        "netname": ""
    },
    "config": {
        "ns": "",
        "name": "",
        "net": {
            "name": ""
        },
        "places": [
            {
                "id": "Start",
                "name": "Start",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "about to get all types from structure",
                "name": "about to get all types from structure",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "about to nmap scan",
                "name": "about to nmap scan",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "After scan",
                "name": "After scan",
                "audit": false,
                "kpi": [{"label": "discovered_ips", "fact": "#discoveredIpCount", "type": "hourly"}],
                "rule": false
            },
            {
                "id": "stop or continue",
                "name": "stop or continue",
                "audit": false,
                "kpi": [],
                "rule": true
            },
            {
                "id": "listening for status messages",
                "name": "listening for status messages",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "status event received",
                "name": "status event received",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "about to specialize",
                "name": "about to specialize",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "specialize attempted",
                "name": "specialize attempted",
                "audit": false,
                "kpi": [{"label": "specialize_ATTEMPTED", "fact": 1, "type": "hourly"}],
                "rule": true
            },
            {
                "id": "End",
                "name": "End",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "force end",
                "name": "force end",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "determine OS category",
                "name": "determine OS category",
                "audit": false,
                "kpi": [],
                "rule": true
            },
            {
                "id": "About to deep detect",
                "name": "About to deep detect",
                "audit": false,
                "kpi": [],
                "rule": false
            },
            {
                "id": "check detect result",
                "name": "check detect result",
                "audit": false,
                "kpi": [{"label": "#currentType", "fact": 1, "type": "hourly"}],
                "rule": true
            },
            {
                "id": "detect ok kpi",
                "name": "detect ok kpi",
                "audit": false,
                "kpi": [{"label": "detect_OK", "fact": 1, "type": "hourly"}],
                "rule": false
            },
            {
                "id": "detect failed kpi",
                "name": "detect failed kpi",
                "audit": false,
                "kpi": [{"label": "detect_FAILED", "fact": 1, "type": "hourly"}],
                "rule": false
            }
        ],
        "transitions": [
            {
                "id": "recv cidr",
                "name": "recv cidr",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["AuthError","ValidationError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Get all types",
                "name": "Get all types",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["AuthError", "FetchError", "ValidationError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Nmap scan",
                "name": "Nmap scan",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["ValidationError", "NmapError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "CreateFilter",
                "name": "CreateFilter",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["ValidationError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Status listener",
                "name": "Status listener",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Handle event",
                "name": "Handle event",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Specialize",
                "name": "Specialize",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Loop exit",
                "name": "Loop exit",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Still receiving",
                "name": "Still receiving",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Forcing to end",
                "name": "Forcing to end",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Guess by port",
                "name": "Guess by port",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["ValidationError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "UnixLike create",
                "name": "UnixLike create",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["ValidationError", "ApiError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Windows create",
                "name": "Windows create",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["ValidationError", "ApiError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Generic create",
                "name": "Generic create",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["ValidationError", "ApiError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Detect",
                "name": "Detect",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": ["ValidationError", "ApiError"],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Detect OK",
                "name": "Detect OK",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "ok pass",
                "name": "ok pass",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "Detect failed",
                "name": "Detect failed",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            },
            {
                "id": "failed pass",
                "name": "failed pass",
                "type": "Pass",
                "connector": "Pass",
                "inMapping": "",
                "outMapping": "",
                "toContinue": [],
                "toStop": [],
                "toRun": [],
                "theDefault": "stop",
                "inSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                },
                "outSpec": {
                    "type": "object",
                    "required": [],
                    "properties": {},
                    "additionalProperties": true
                }
            }
        ],
        "arcs": [
            {
                "id": "e34d57b8-c532-4aaf-bbf1-9566e5c3eda0",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "08e03592-b10c-4935-b526-eef59f973dd5",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "46bc4b5e-0455-45eb-ae69-208819a16d23",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "7d3a7548-30bc-4bd8-b811-a0a422e3126d",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "23ba7571-d246-4ead-bab3-359ae33c889c",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "00003b42-fdd1-4476-9bfb-8e09d335f0d0",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "6dae4c4c-eaee-4a07-a8cf-88331ff067ee",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "03b61a39-6e84-4239-812a-629de01a90ad",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "71314a9b-9b12-431c-a5ba-6f1a28841de1",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "80dfc380-0427-42a9-9641-b315a257a3ef",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "7a78b5d7-83c5-464b-b1b9-a419d3df15cf",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "15cc43e0-359a-4123-866d-b9767ac95605",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "16a0c78b-1e0c-4bd8-b274-32d281d90db5",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "6d2a433a-eb1f-4971-9aa6-ede36ef0c0b2",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "a911f25f-eeeb-4189-9f1f-ec3f1d229de7",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "4eccd0e5-a710-4e55-bb90-d752d318a8be",
                "rule": "shouldTerminate",
                "ruleType": "local",
                "mapping": "dataDictionary.countDown === 0"
            },
            {
                "id": "3c2ddaea-43fc-41b4-8dad-65502c16a268",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "424b601a-8c3e-4b4e-834f-6508bdb8eaa5",
                "rule": "shouldContinue",
                "ruleType": "local",
                "mapping": "dataDictionary.countDown > 0"
            },
            {
                "id": "f86ca3c0-2176-4807-9fc3-307e03584b4e",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "cb245e18-7db0-46ec-a1ce-811a2172cbaa",
                "rule": "forceTerminate",
                "ruleType": "local",
                "mapping": "R.and(R.compose(R.equals(0), R.length) (dataDictionary.discoveredIps), R.equals(0, dataDictionary.countDown))"
            },
            {
                "id": "9cce4616-f05a-45f2-a9fd-4952dd3c7237",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "c7e0cc4b-b3ff-45a0-a036-6fbfce540623",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "6ed7cdec-f695-439f-b10d-70c389207ac0",
                "rule": "guessCheck",
                "ruleType": "local",
                "mapping": "R.length(dataDictionary.discoveredIps) > 0"
            },
            {
                "id": "65820a08-0428-45e1-9751-195001510ec5",
                "rule": "unixType",
                "ruleType": "local",
                "mapping": "dataDictionary.currentGuess === 'unix'"
            },
            {
                "id": "bedc9ae1-6bd5-4e02-9ff2-51a5372e4036",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "8884d002-a025-4c16-906a-e515d8754e09",
                "rule": "windowsType",
                "ruleType": "local",
                "mapping": "dataDictionary.currentGuess === 'windows'"
            },
            {
                "id": "ecf9e721-97f4-4886-85a4-d8d2e560ed5e",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "a82741d9-62d1-4107-9073-7551bf44d0e3",
                "rule": "normalType",
                "ruleType": "local",
                "mapping": "dataDictionary.currentGuess === 'generic'"
            },
            {
                "id": "4df8ff49-23bb-4dec-a7d5-e05af6ecb054",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "56f62caf-d507-4186-9e83-53f7f7cc99c7",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "f04d4359-7020-44b8-9d15-36d1691a42e9",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "c148c2a9-7848-465c-8112-b952335e8d99",
                "rule": "detectOK",
                "ruleType": "local",
                "mapping": "R.compose(R.not, R.has('error')) (dataDictionary.deeplyDetected.specialized)"
            },
            {
                "id": "2821e98f-6b14-41e8-9d04-1e81bfc04f6f",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "a84604e7-a7f6-4821-b741-006a29a73e33",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "119b6e40-bee8-4d0f-a3ae-1e3470a0cff0",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "15dd91ca-712b-4bbd-897d-5721ea8186af",
                "rule": "detectFAIL",
                "ruleType": "local",
                "mapping": "R.has('error', dataDictionary.deeplyDetected.specialized)"
            },
            {
                "id": "d08a532f-6b41-46e9-b7ff-2199888a4d93",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "6fbe4cde-a46f-4985-a11c-91baa420eae5",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            },
            {
                "id": "5c74918d-355c-45d8-888c-d3ee5f0b01eb",
                "rule": "",
                "ruleType": "none",
                "mapping": ""
            }
        ],
        "created": ""
    }
};
