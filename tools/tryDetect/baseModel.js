module.exports = {
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
        "id": "about to specialize",
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
        "id": "detect went ok",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "about to remove generic instance",
        "im": 0,
        "em": 0,
        "capacity": 0
      },
      {
        "id": "failed specialize End",
        "im": 0,
        "em": 1,
        "capacity": 0
      },
      {
        "id": "No specialize End",
        "im": 0,
        "em": 1,
        "capacity": 0
      },
      {
        "id": "End",
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
        "id": "detect ok"
      },
      {
        "id": "detect nok"
      },
      {
        "id": "removeInstance"
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
        "id": "162159da-5a71-439e-ae2b-bd57831b94d4",
        "from": "Start",
        "to": "recv",
        "weight": 1
      },
      {
        "id": "c2cd38f3-315c-4ee5-90e8-3bc9a8320dfb",
        "from": "recv",
        "to": "about to get all types",
        "weight": 1
      },
      {
        "id": "89709ddf-c6a6-44ee-9c15-b35e2da7efd2",
        "from": "about to get all types",
        "to": "getAllTypes",
        "weight": 1
      },
      {
        "id": "0c66d004-9992-49b1-902a-e9c280ab5b50",
        "from": "getAllTypes",
        "to": "all types",
        "weight": 1
      },
      {
        "id": "680f4e56-d112-4e54-a088-e08c37813c15",
        "from": "all types",
        "to": "fetchInstance",
        "weight": 1
      },
      {
        "id": "57e86900-c36c-4d06-ada6-ddd14666a810",
        "from": "fetchInstance",
        "to": "about to create",
        "weight": 1
      },
      {
        "id": "cdd62b20-51cb-4dfa-8045-772e0b12fe2d",
        "from": "about to create",
        "to": "createGenericInstance",
        "weight": 1
      },
      {
        "id": "6b48f0b3-4f78-46c1-a9a1-13438ce5bf47",
        "from": "createGenericInstance",
        "to": "instance created",
        "weight": 1
      },
      {
        "id": "d69fbebf-077a-4847-9140-d5760da1eeec",
        "from": "instance created",
        "to": "detect",
        "weight": 1
      },
      {
        "id": "8cd123f6-82b8-4ae9-a59e-4f5bb99b6db6",
        "from": "detect",
        "to": "check detect result",
        "weight": 1
      },
      {
        "id": "273b383b-378d-4ed9-a291-3b0d5f8abb2e",
        "from": "recv",
        "to": "listening for status",
        "weight": 1
      },
      {
        "id": "a1eb380f-992d-4f8b-8d84-1d39b0676bfa",
        "from": "listening for status",
        "to": "handleStatusMessage",
        "weight": 1
      },
      {
        "id": "ed42fbcb-7cce-4f69-b1af-70b7a225dd67",
        "from": "handleStatusMessage",
        "to": "about to specialize",
        "weight": 1
      },
      {
        "id": "c8f593b1-8eaa-4b8a-ba05-f4b1104deedd",
        "from": "about to specialize",
        "to": "specialize",
        "weight": 1
      },
      {
        "id": "856010c8-35c2-409c-a183-95406be0c348",
        "from": "specialize",
        "to": "check specialize result",
        "weight": 1
      },
      {
        "id": "895806f2-0b93-486d-9c81-2bb4f7702c35",
        "from": "check detect result",
        "to": "detect ok",
        "weight": 1
      },
      {
        "id": "6808b240-cbae-4802-bb2f-437a529a5569",
        "from": "detect ok",
        "to": "detect went ok",
        "weight": 1
      },
      {
        "id": "0594df6a-9fec-4742-8b56-a8d92f9fe9d9",
        "from": "check detect result",
        "to": "detect nok",
        "weight": 1
      },
      {
        "id": "8096f255-e120-4963-82c7-4ee1ff946a86",
        "from": "detect nok",
        "to": "about to remove generic instance",
        "weight": 1
      },
      {
        "id": "3eca0667-5ecc-4bfa-abd0-bfdce16def16",
        "from": "about to remove generic instance",
        "to": "removeInstance",
        "weight": 1
      },
      {
        "id": "f8845206-aa18-4ee9-b289-a04b52740f69",
        "from": "removeInstance",
        "to": "No specialize End",
        "weight": 1
      },
      {
        "id": "5bf985b9-77b1-492a-baf3-cb251bf89e32",
        "from": "check specialize result",
        "to": "specialize not ok",
        "weight": 1
      },
      {
        "id": "b98f6151-d23c-42da-b903-196592a94f1d",
        "from": "specialize not ok",
        "to": "failed specialize End",
        "weight": 1
      },
      {
        "id": "a36c4597-937a-40c1-96d1-9401dce049fd",
        "from": "check specialize result",
        "to": "specialize ok",
        "weight": 1
      },
      {
        "id": "29c8ab0b-f848-4957-8ec2-523bd33d25a3",
        "from": "specialize ok",
        "to": "End",
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
        "id": "about to specialize",
        "tokens": 0
      },
      {
        "id": "check specialize result",
        "tokens": 0
      },
      {
        "id": "detect went ok",
        "tokens": 0
      },
      {
        "id": "about to remove generic instance",
        "tokens": 0
      },
      {
        "id": "failed specialize End",
        "tokens": 0
      },
      {
        "id": "No specialize End",
        "tokens": 0
      },
      {
        "id": "End",
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
        "id": "about to specialize",
        "name": "about to specialize",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "check specialize result",
        "name": "check specialize result",
        "audit": false,
        "kpi": [],
        "rule": true
      },
      {
        "id": "detect went ok",
        "name": "detect went ok",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "about to remove generic instance",
        "name": "about to remove generic instance",
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
        "id": "No specialize End",
        "name": "No specialize End",
        "audit": false,
        "kpi": [],
        "rule": false
      },
      {
        "id": "End",
        "name": "End",
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
        "id": "fetchInstance",
        "name": "fetchInstance",
        "type": "Script",
        "connector": "Structure.getInstance",
        "inMapping": "() => {\n    \"use strict\";\n    let url = util.format('http://%s:%s/api/Instances/uuid/%s',\n        cache.get(\"Structure_api_host\"), cache.get(\"Structure_api_port\"),\n        cache.get(\"Structure_getInstance_uuid\"));\n\n    return fetch(url)\n        .then(result => result.json())\n        .then(json => {\n            options[cache.get(\"Structure_getInstance_result_dictEntry\")] = json;\n            return options;\n        });\n}",
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
        "id": "createGenericInstance",
        "name": "createGenericInstance",
        "type": "Local",
        "connector": "structure.createGenericInstance",
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
        "id": "handleStatusMessage",
        "name": "handleStatusMessage",
        "type": "Input",
        "connector": "Input",
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
        "id": "162159da-5a71-439e-ae2b-bd57831b94d4",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "c2cd38f3-315c-4ee5-90e8-3bc9a8320dfb",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "89709ddf-c6a6-44ee-9c15-b35e2da7efd2",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "0c66d004-9992-49b1-902a-e9c280ab5b50",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "680f4e56-d112-4e54-a088-e08c37813c15",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "57e86900-c36c-4d06-ada6-ddd14666a810",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "cdd62b20-51cb-4dfa-8045-772e0b12fe2d",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "6b48f0b3-4f78-46c1-a9a1-13438ce5bf47",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "d69fbebf-077a-4847-9140-d5760da1eeec",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "8cd123f6-82b8-4ae9-a59e-4f5bb99b6db6",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "273b383b-378d-4ed9-a291-3b0d5f8abb2e",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "a1eb380f-992d-4f8b-8d84-1d39b0676bfa",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "ed42fbcb-7cce-4f69-b1af-70b7a225dd67",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "c8f593b1-8eaa-4b8a-ba05-f4b1104deedd",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "856010c8-35c2-409c-a183-95406be0c348",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "895806f2-0b93-486d-9c81-2bb4f7702c35",
        "rule": "detectOK",
        "ruleType": "local",
        "mapping": "R.not(R.has('error',dataDictionary.detectResult))"
      },
      {
        "id": "6808b240-cbae-4802-bb2f-437a529a5569",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "0594df6a-9fec-4742-8b56-a8d92f9fe9d9",
        "rule": "detectNOK",
        "ruleType": "local",
        "mapping": "R.has('error', dataDictionary.detectResult)"
      },
      {
        "id": "8096f255-e120-4963-82c7-4ee1ff946a86",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "3eca0667-5ecc-4bfa-abd0-bfdce16def16",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "f8845206-aa18-4ee9-b289-a04b52740f69",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "5bf985b9-77b1-492a-baf3-cb251bf89e32",
        "rule": "specializeNOK",
        "ruleType": "local",
        "mapping": "R.has('error', dataDictionary.specializeResult) || R.equals(dataDictionary.specializeResult, \"Arguments didn't validate against schema.\")"
      },
      {
        "id": "b98f6151-d23c-42da-b903-196592a94f1d",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      },
      {
        "id": "a36c4597-937a-40c1-96d1-9401dce049fd",
        "rule": "specializeOK",
        "ruleType": "local",
        "mapping": "!R.has('error', dataDictionary.specializeResult) && !R.equals(dataDictionary.specializeResult, \"Arguments didn't validate against schema.\")"
      },
      {
        "id": "29c8ab0b-f848-4957-8ec2-523bd33d25a3",
        "rule": "",
        "ruleType": "none",
        "mapping": ""
      }
    ],
    "created": "Wed Jul 12 2017 13:25:37 GMT+0200"
  }
};