module.exports =  {
  "ns": "structure",
  "netName": "tryDetect",
  "configName": "tryDetectConfig",
  "processName": "tryDetectProcess",
  "start": "recv",
  "model": {
    "places": [
      {
        "id": "Start",
        "im": 1,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "about to get all types",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "all types",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "about to create",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "instance created",
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
        "id": "listening for status",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "received status message",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "check specialize result",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "Instance removed End",
        "im": 0,
        "em": 1,
        "capacity": 0
      },
      {
        "id": "detect went ok",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "Detect error End",
        "im": 0,
        "em": 1,
        "capacity": 0
      },
      {
        "id": "failed specialize End",
        "im": 0,
        "em": 1,
        "capacity": 0
      },
      {
        "id": "specialized End",
        "im": 0,
        "em": 1,
        "capacity": 0
      }
    ],
    "transitions": [
      {
        "id": "recv"
      },
      {
        "id": "getAllTypes"
      },
      {
        "id": "fetchInstance"
      },
      {
        "id": "createGenericInstance"
      },
      {
        "id": "detect"
      },
      {
        "id": "handleStatusMessage"
      },
      {
        "id": "specialize"
      },
      {
        "id": "removeInstance"
      },
      {
        "id": "detect ok"
      },
      {
        "id": "detect nok"
      },
      {
        "id": "specialize not ok"
      },
      {
        "id": "specialize ok"
      }
    ],
    "arcs": [
      {
        "id": "96ed2762-7554-4f74-9600-c7d39a5130f3",
        "from": "Start",
        "to": "recv",
        "weight": 1
      },
      {
        "id": "512701c4-7715-41ff-878d-1939d407de37",
        "from": "recv",
        "to": "about to get all types",
        "weight": 1
      },
      {
        "id": "b8175e72-7c59-416b-9d74-fbb511e01154",
        "from": "about to get all types",
        "to": "getAllTypes",
        "weight": 1
      },
      {
        "id": "af894245-4227-402b-9bd3-46b2ef010e19",
        "from": "getAllTypes",
        "to": "all types",
        "weight": 1
      },
      {
        "id": "7d722926-48e0-4f02-b2f7-e7a3783742e8",
        "from": "all types",
        "to": "fetchInstance",
        "weight": 1
      },
      {
        "id": "5985d539-e9c2-4d9f-a6c7-b6d3dc7e8eec",
        "from": "fetchInstance",
        "to": "about to create",
        "weight": 1
      },
      {
        "id": "183d31c9-7101-4598-94fb-05a8068e450a",
        "from": "about to create",
        "to": "createGenericInstance",
        "weight": 1
      },
      {
        "id": "f408ec2f-c70c-44ec-bdac-e1af5ecf0c04",
        "from": "createGenericInstance",
        "to": "instance created",
        "weight": 1
      },
      {
        "id": "6f46f7df-872f-4452-abad-d8ca7b930b8a",
        "from": "instance created",
        "to": "detect",
        "weight": 1
      },
      {
        "id": "8e926d70-b192-48cc-95ce-fe4fbf06b7b1",
        "from": "detect",
        "to": "check detect result",
        "weight": 1
      },
      {
        "id": "82645a81-1d5d-4940-b2f7-5a08625a2af5",
        "from": "recv",
        "to": "listening for status",
        "weight": 1
      },
      {
        "id": "4ddd55f0-eced-48c6-a55a-10475f8efd28",
        "from": "listening for status",
        "to": "handleStatusMessage",
        "weight": 1
      },
      {
        "id": "d91a28ac-c26c-4af4-a24e-a56b3bfebd6a",
        "from": "handleStatusMessage",
        "to": "received status message",
        "weight": 1
      },
      {
        "id": "d1c7e213-9652-4528-a892-b45c674579f4",
        "from": "received status message",
        "to": "specialize",
        "weight": 1
      },
      {
        "id": "80b9a837-027a-4e37-b0f9-3a4ca55ffbdb",
        "from": "specialize",
        "to": "check specialize result",
        "weight": 1
      },
      {
        "id": "be1a1548-4438-4708-8d07-afd02d7766bd",
        "from": "received status message",
        "to": "removeInstance",
        "weight": 1
      },
      {
        "id": "153200aa-8297-488f-87ce-eb68a98837b7",
        "from": "removeInstance",
        "to": "Instance removed End",
        "weight": 1
      },
      {
        "id": "321c4cd1-2bbf-44dc-94a9-0ba07c6e2f15",
        "from": "check detect result",
        "to": "detect ok",
        "weight": 1
      },
      {
        "id": "026c8ffb-d17b-4e68-8b3a-ce206b737fc6",
        "from": "detect ok",
        "to": "detect went ok",
        "weight": 1
      },
      {
        "id": "ace0d4e8-3f8a-4edb-a169-4367013a4eaa",
        "from": "check detect result",
        "to": "detect nok",
        "weight": 1
      },
      {
        "id": "fdc45833-1909-4c65-b9fe-96d0bd17141e",
        "from": "detect nok",
        "to": "Detect error End",
        "weight": 1
      },
      {
        "id": "7b86b2b1-05d0-44e0-9f4c-18154c5a9e86",
        "from": "check specialize result",
        "to": "specialize not ok",
        "weight": 1
      },
      {
        "id": "960a6951-5ce9-445f-809c-8c5428b719c2",
        "from": "specialize not ok",
        "to": "failed specialize End",
        "weight": 1
      },
      {
        "id": "a55dd597-9a3c-42c4-ad2d-eb151774e84e",
        "from": "check specialize result",
        "to": "specialize ok",
        "weight": 1
      },
      {
        "id": "4cf38a31-ebb4-4cfe-83f4-c4cba2dcf1c8",
        "from": "specialize ok",
        "to": "specialized End",
        "weight": 1
      }
    ],
    "markings": [
      {
        "id": "Start",
        "tokens": 1
      },
      {
        "id": "about to get all types",
        "tokens": 0
      },
      {
        "id": "all types",
        "tokens": 0
      },
      {
        "id": "about to create",
        "tokens": 0
      },
      {
        "id": "instance created",
        "tokens": 0
      },
      {
        "id": "check detect result",
        "tokens": 0
      },
      {
        "id": "listening for status",
        "tokens": 0
      },
      {
        "id": "received status message",
        "tokens": 0
      },
      {
        "id": "check specialize result",
        "tokens": 0
      },
      {
        "id": "Instance removed End",
        "tokens": 0
      },
      {
        "id": "detect went ok",
        "tokens": 0
      },
      {
        "id": "Detect error End",
        "tokens": 0
      },
      {
        "id": "failed specialize End",
        "tokens": 0
      },
      {
        "id": "specialized End",
        "tokens": 0
      }
    ],
    "netname": "tryDetect"
  },
  "config": {
    "ns": "structure",
    "name": "tryDetectConfig",
    "net": {
      "name": "tryDetect"
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
        "id": "about to get all types",
        "name": "about to get all types",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "all types",
        "name": "all types",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "about to create",
        "name": "about to create",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "instance created",
        "name": "instance created",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "check detect result",
        "name": "check detect result",
        "audit": false,
        "kpi": [],
        "rule": true
      },
      {
        "id": "listening for status",
        "name": "listening for status",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "received status message",
        "name": "received status message",
        "audit": true,
        "kpi": [],
        "rule": true
      },
      {
        "id": "check specialize result",
        "name": "check specialize result",
        "audit": false,
        "kpi": [],
        "rule": true
      },
      {
        "id": "Instance removed End",
        "name": "Instance removed End",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "detect went ok",
        "name": "detect went ok",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "Detect error End",
        "name": "Detect error End",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "failed specialize End",
        "name": "failed specialize End",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "specialized End",
        "name": "specialized End",
        "audit": false,
        "kpi": [],
        "rule": false
      }
    ],
    "transitions": [
      {
        "id": "recv",
        "name": "recv",
        "type": "Input",
        "connector": "Input",
        "inMapping": "(payload) => {\n    aeonCache.set(\"Structure_api_token\", payload.token);\n    let obj = {\n        token: payload.token,\n        typeName: payload.typeName,\n        instanceData: payload.data\n    }\n    obj[aeonCache.get(\"Structure_getInstance_uuid_dictEntry\")] = payload.instanceUuid;\n\n    return obj;\n}",
        "outMapping": "",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {
            "instanceUuid": {
              "type": "string"
            },
            "typeName": {
              "type": "string"
            },
            "token": {
              "type": "string"
            },
            "data": {
              "type": "object",
              "properties": {},
              "required": [],
              "additionalProperties": true
            }
          },
          "required": [
            "instanceUuid",
            "typeName",
            "token",
            "data"
          ],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "getAllTypes",
        "name": "getAllTypes",
        "type": "Script",
        "connector": "Structure.allTypes",
        "inMapping": "() => {\n    \"use strict\";\n    let url = util.format('http://%s:%s/api/Entities', cache.get(\"Structure_api_host\"), cache.get(\"Structure_api_port\"));\n    return fetch(url)\n        .then(result => result.json())\n        .then(json => {\n            cache.set(cache.get(\"Structure_allTypes_result_cacheEntry\"), json);\n            return options;\n        });\n}",
        "outMapping": "",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "fetchInstance",
        "name": "fetchInstance",
        "type": "Script",
        "connector": "Structure.getInstance",
        "inMapping": "() => {\n    \"use strict\";\n    let url = util.format('http://%s:%s/api/Instances/uuid/%s',\n        cache.get(\"Structure_api_host\"), cache.get(\"Structure_api_port\"),\n        options[cache.get(\"Structure_getInstance_uuid_dictEntry\")]);\n\n    return fetch(url)\n        .then(result => result.json())\n        .then(json => {\n            options[cache.get(\"Structure_getInstance_result_dictEntry\")] = json;\n            return options;\n        });\n}",
        "outMapping": "",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "createGenericInstance",
        "name": "createGenericInstance",
        "type": "Local",
        "connector": "structure.createGenericInstance",
        "inMapping": "() => {\n    return {\n        db: aeonCache.get(aeonCache.get(\"Structure_allTypes_result_cacheEntry\")),\n        token: aeonCache.get(\"Structure_api_token\"),\n        host: aeonCache.get(\"Structure_api_host\"),\n        port: aeonCache.get(\"Structure_api_port\"),\n        existingInstance: dataDictionary[aeonCache.get(\"Structure_getInstance_result_dictEntry\")],\n        requestedType: dataDictionary.typeName,\n        relation: 'HAS',\n        data: dataDictionary.instanceData\n    };\n}",
        "outMapping": "(data) => {\n    dataDictionary[aeonCache.get(\"Structure_detect_instance_uuid_dictEntry\")] = data;\n    return dataDictionary;\n}",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {
            "token": {
              "type": "string"
            },
            "host": {
              "type": "string"
            },
            "port": {
              "type": "integer"
            },
            "db": {
              "type": "array",
              "minItems": 1,
              "items": {
                "type": "object"
              }
            },
            "existingInstance": {
              "type": "object"
            },
            "requestedType": {
              "type": "string"
            },
            "relation": {
              "type": "string"
            },
            "data": {
              "type": "object"
            }
          },
          "required": [
            "token",
            "db",
            "host",
            "port",
            "existingInstance",
            "requestedType"
          ],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "string"
        }
      },
      {
        "id": "detect",
        "name": "detect",
        "type": "Script",
        "connector": "Structure.detect",
        "inMapping": "() => {\n    \"use strict\";\n    let url = util.format('http://%s:%s/api/Instances/exec/Detect/on/%s?access_token=%s',\n        cache.get(\"Structure_api_host\"), cache.get(\"Structure_api_port\"),\n        options[cache.get(\"Structure_detect_instance_uuid_dictEntry\")],\n        cache.get(\"Structure_api_token\"));\n\n    let args = {\n        capability: 'Detect',\n        token: cache.get(\"Structure_api_token\")\n    };\n\n    return fetch(url, {\n        method: 'POST',\n        body: 'token='+args.token,\n        headers: {'Content-Type': 'application/x-www-form-urlencoded'}\n    })\n        .then(result => result.json())\n        .then(json => {\n            options[cache.get(\"Structure_detect_result_dictEntry\")] = json;\n            return options;\n        });\n}",
        "outMapping": "",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "handleStatusMessage",
        "name": "handleStatusMessage",
        "type": "Input",
        "connector": "Input",
        "inMapping": "(payload) => {\n    let jobs = payload.data.taskObj.jobs;\n    let result = R.find(job => {\n        if(R.isNil(job.result)) return false;\n        let str = R.trim(job.result.stdout);\n        return (str !== \"\" && str !== \"\\n\");\n    },jobs);\n\n    let obj = {};\n    obj.detectOutputObject = result;\n\n    if(R.isNil(result)) {\n        obj[aeonCache.get(\"Structure_specialize_specialization_dictEntry\")] = \"\";\n\t\tdataDictionary.jobStatus = 'FAILED';\n\t\tobj.jobStatus = 'FAILED';\n        return obj;\n    } else {\n\t\tdataDictionary.jobStatus = R.defaultTo('FAILED', result.status);\n\t\tobj.jobStatus = R.defaultTo('FAILED', result.status);\n\t}\n\n    let found = R.find(R.propEq('name', result.result.stdout), aeonCache.get(aeonCache.get(\"Structure_allTypes_result_cacheEntry\")));\n    if(R.isNil(found)) {\n        obj[aeonCache.get(\"Structure_specialize_specialization_dictEntry\")] = \"\";\n    } else {\n        obj[aeonCache.get(\"Structure_specialize_specialization_dictEntry\")] = found.meta.uuid;\n    }\n    \n    return obj;\n}",
        "outMapping": "",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {
            "event": {
              "type": "string",
              "pattern": "taskFinished"
            },
            "source": {
              "type": "string",
              "pattern": "TaskManager"
            }
          },
          "required": [
            "event",
            "source"
          ],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "specialize",
        "name": "specialize",
        "type": "Script",
        "connector": "Structure.specialize",
        "inMapping": "() => {\n    \"use strict\";\n    let url = util.format('http://%s:%s/api/Instances/specialize?access_token=%s',\n        cache.get(\"Structure_api_host\"), cache.get(\"Structure_api_port\"),\n        cache.get(\"Structure_api_token\"));\n\n    let instanceUuid = options[cache.get(\"Structure_specialize_origin_uuid_dictEntry\")];\n    let isaUuid = options[cache.get(\"Structure_specialize_specialization_dictEntry\")];\n    return fetch(url, {\n        method: 'PUT',\n        body: util.format('instanceUuid=%s&isaUuid=%s', instanceUuid, isaUuid),\n        headers: {'Content-Type': 'application/x-www-form-urlencoded'}\n    })\n        .then(result => result.json())\n        .then(json => {\n            options[cache.get(\"Structure_specialize_result_dictEntry\")] = json;\n            return options;\n        });\n}",
        "outMapping": "",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "removeInstance",
        "name": "removeInstance",
        "type": "Script",
        "connector": "Structure.removeInstance",
        "inMapping": "() => {\n    \"use strict\";\n    let url = util.format('http://%s:%s/api/Instances/%s?access_token=%s',\n        cache.get(\"Structure_api_host\"), cache.get(\"Structure_api_port\"),\n        options[cache.get(\"Structure_removeInstance_uuid_dictEntry\")],\n        cache.get(\"Structure_api_token\"));\n\n    return fetch(url, {method:\"DELETE\"})\n        .then(result => result.json())\n        .then(json => {\n            options[cache.get(\"Structure_removeInstance_result_dictEntry\")] = json;\n            return options;\n        });\n}",
        "outMapping": "",
        "toContinue": [],
        "toStop": [],
        "toRun": [],
        "theDefault": "stop",
        "inSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "detect ok",
        "name": "detect ok",
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
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "detect nok",
        "name": "detect nok",
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
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "specialize not ok",
        "name": "specialize not ok",
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
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      },
      {
        "id": "specialize ok",
        "name": "specialize ok",
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
          "properties": {},
          "required": [],
          "additionalProperties": true
        },
        "outSpec": {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": true
        }
      }
    ],
    "arcs": [
      {
        "id": "96ed2762-7554-4f74-9600-c7d39a5130f3",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "512701c4-7715-41ff-878d-1939d407de37",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "b8175e72-7c59-416b-9d74-fbb511e01154",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "af894245-4227-402b-9bd3-46b2ef010e19",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "7d722926-48e0-4f02-b2f7-e7a3783742e8",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "5985d539-e9c2-4d9f-a6c7-b6d3dc7e8eec",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "183d31c9-7101-4598-94fb-05a8068e450a",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "f408ec2f-c70c-44ec-bdac-e1af5ecf0c04",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "6f46f7df-872f-4452-abad-d8ca7b930b8a",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "8e926d70-b192-48cc-95ce-fe4fbf06b7b1",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "82645a81-1d5d-4940-b2f7-5a08625a2af5",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "4ddd55f0-eced-48c6-a55a-10475f8efd28",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "d91a28ac-c26c-4af4-a24e-a56b3bfebd6a",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "d1c7e213-9652-4528-a892-b45c674579f4",
        "rule": "successfulTask",
        "ruleType": "local",
        "mapping": "dataDictionary.jobStatus === 'FINISHED'"
      },
      {
        "id": "80b9a837-027a-4e37-b0f9-3a4ca55ffbdb",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "be1a1548-4438-4708-8d07-afd02d7766bd",
        "rule": "nonSuccessfulTask",
        "ruleType": "local",
        "mapping": "dataDictionary.jobStatus !== 'FINISHED'"
      },
      {
        "id": "153200aa-8297-488f-87ce-eb68a98837b7",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "321c4cd1-2bbf-44dc-94a9-0ba07c6e2f15",
        "rule": "detectOK",
        "ruleType": "local",
        "mapping": "R.not(R.has('error',dataDictionary.detectResult))"
      },
      {
        "id": "026c8ffb-d17b-4e68-8b3a-ce206b737fc6",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "ace0d4e8-3f8a-4edb-a169-4367013a4eaa",
        "rule": "detectNOK",
        "ruleType": "local",
        "mapping": "R.has('error', dataDictionary.detectResult)"
      },
      {
        "id": "fdc45833-1909-4c65-b9fe-96d0bd17141e",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "7b86b2b1-05d0-44e0-9f4c-18154c5a9e86",
        "rule": "specializeNOK",
        "ruleType": "local",
        "mapping": "R.has('error', dataDictionary.specializeResult) || R.equals(dataDictionary.specializeResult, \"Arguments didn't validate against schema.\")"
      },
      {
        "id": "960a6951-5ce9-445f-809c-8c5428b719c2",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "a55dd597-9a3c-42c4-ad2d-eb151774e84e",
        "rule": "specializeOK",
        "ruleType": "local",
        "mapping": "!R.has('error', dataDictionary.specializeResult) && !R.equals(dataDictionary.specializeResult, \"Arguments didn't validate against schema.\")"
      },
      {
        "id": "4cf38a31-ebb4-4cfe-83f4-c4cba2dcf1c8",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      }
    ],
    "created": "Mon Aug 14 2017 15:26:38 GMT+0200"
  }
}