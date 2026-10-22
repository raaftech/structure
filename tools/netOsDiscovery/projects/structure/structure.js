"use strict";

const R = require("ramda");
const nmapLib = require("./nmapLib");
const RestConnector = require("./restConnector");
const graphLib = require("./graphLib");
const structureLib = require("./structureLib");

const getAllTypes = (input) => {
  const rest = new RestConnector(
    R.defaultTo("localhost", R.path(["api", "server"], input)),
    R.defaultTo(3000, R.path(["api", "port"], input)),
  );
  let creds;

  let email = R.defaultTo("a@b.nl", R.path(["api", "email"], input));
  let pwd = R.defaultTo("test", R.path(["api", "pwd"], input));
  return rest
    .login(email, pwd)
    .then((credentials) => {
      creds = credentials;
      return rest.getAllTypes().then((allTypes) => {
        return {
          db: allTypes,
          credentials: creds,
        };
      });
    })
    .catch((ex) => {
      ex.name = "AuthError";
      throw ex;
    });
};

//input {cidr: ''}
//generic: the-discover-function
const nmap = (data) => {
  return nmapLib.nmapScan(data.cidr, R.defaultTo("-p22,445,137,138", data.nmapArgs)).catch((ex) => {
    ex.name = "NmapError";
    throw ex;
  });
};

//{all: [], created: []}
//generic: transform-discodata-to-types
const createFilter = (data) => {
  let createdIps = [];
  let baseType = graphLib.findTypeByName(data.db, "Ipv4Address");

  R.forEach((item) => {
    let instance = graphLib.createInstance(baseType, {
      name: "Ipv4Address",
      desc: baseType.desc,
      addr: item.ip,
      reachable: true,
    });
    createdIps.push(instance);
  }, data.ips);

  return Promise.resolve(createdIps);
};

//generic: structure-guess
const guessByPort = (data) => {
  let entity = data.currentIp;
  let hasPort = (port, ent) => {
    let openPorts = R.prop("openPorts", ent);
    openPorts = R.reject(R.isNil, openPorts);
    if (R.length(openPorts) === 0) return false;

    return R.pipe(R.filter(R.propEq("port", port)), R.length, R.lt(0))(openPorts);
  };
  let createReply = R.curry((ent, ip, guess) => {
    return {
      guess: guess,
      entity: ent,
      ipData: ip,
    };
  });
  let cr = createReply(data.entity, data.currentIp);

  if (entity.openPorts.length === 0) return Promise.resolve(cr("generic"));
  if (hasPort(445, entity)) return Promise.resolve(cr("windows"));
  if (hasPort(137, entity)) return Promise.resolve(cr("windows"));
  if (hasPort(138, entity)) return Promise.resolve(cr("windows"));
  if (hasPort(22, entity)) return Promise.resolve(cr("unix"));
  return Promise.resolve(cr("generic"));
};

//generic all three: structure-create-instance of guessed type
const unixCreate = (input) => {
  const rest = new RestConnector(
    R.defaultTo("localhost", R.path(["api", "server"], input)),
    R.defaultTo(3000, R.path(["api", "port"], input)),
  );
  rest.setToken(input.token);
  let computer = structureLib.createComputer(input.db, "UnixLike", input.data.entity, input.data.ipData, true, false);
  return rest
    .createInstance({ entities: computer })
    .then((created) => {
      if (R.is(String, created)) created = JSON.parse(created);
      return {
        computer: computer,
        result: created,
      };
    })
    .catch((ex) => {
      ex.name = "ApiError";
      throw ex;
    });
};

const windowsCreate = (input) => {
  const rest = new RestConnector(
    R.defaultTo("localhost", R.path(["api", "server"], input)),
    R.defaultTo(3000, R.path(["api", "port"], input)),
  );
  rest.setToken(input.token);
  let computer = structureLib.createComputer(input.db, "Windows", input.data.entity, input.data.ipData, false, true);
  return rest
    .createInstance({ entities: computer })
    .then((created) => {
      if (R.is(String, created)) created = JSON.parse(created);
      return {
        computer: computer,
        result: created,
      };
    })
    .catch((ex) => {
      ex.name = "ApiError";
      throw ex;
    });
};

const genericCreate = (input) => {
  const rest = new RestConnector(
    R.defaultTo("localhost", R.path(["api", "server"], input)),
    R.defaultTo(3000, R.path(["api", "port"], input)),
  );
  rest.setToken(input.token);
  let computer = structureLib.createComputer(
    input.db,
    "OperatingSystem",
    input.data.entity,
    input.data.ipData,
    false,
    false,
  );
  return rest
    .createInstance({ entities: computer })
    .then((created) => {
      if (R.is(String, created)) created = JSON.parse(created);
      return {
        computer: computer,
        result: created,
      };
    })
    .catch((ex) => {
      ex.name = "ApiError";
      throw ex;
    });
};

//generic: structure-detect
const deepDetect = (input) => {
  const rest = new RestConnector(
    R.defaultTo("localhost", R.path(["api", "server"], input)),
    R.defaultTo(3000, R.path(["api", "port"], input)),
  );
  rest.setToken(input.token);

  let isOS = (ent) => {
    return ent.name === "UnixLike" || ent.name === "Windows" || ent.name === "OperatingSystem";
  };

  let uuid = R.path(["meta", "uuid"], R.find(isOS, input.entry.computer));
  return rest
    .runCapability(uuid, "Detect", input.token)
    .then((result) => {
      //console.log('DEEP DETECT: '+JSON.stringify(result, undefined, 2));
      return {
        entity: uuid,
        specialized: result,
      };
    })
    .catch((ex) => {
      ex.name = "ApiError";
      throw ex;
    });
};

//{"status":"FINISHED",
// "originName":"UnixLike",
// "originUuid":"3fb1c82b-17ce-4ac3-bb7a-fa6f2f87ea84",
// "instrument":"py2discovery","capability":"Detect","timeAllocated":10000,
// "result":{"process":"scp root@172.17.1.1:/tmp/", "returnCode":0,"stdout":"Linux\n","stderr":""},
// "process":"scp /tmp/"}
const handleStatusMessage = (input) => {
  let jobs = R.path(["item", "data", "taskObj", "jobs"], input);
  if (R.isNil(jobs)) {
    return Promise.resolve({
      status: "ERROR",
      originName: "",
      origin: "",
      specialization: "UNKNOWN_SPECIALIZATION",
    });
  }
  let result = R.find((job) => {
    if (R.isNil(job.result)) return false;
    let str = R.trim(job.result.stdout);
    return str !== "" && str !== "\n";
  }, jobs);

  if (R.isNil(result)) {
    return Promise.resolve({
      status: R.compose(R.prop("status"), R.head)(jobs),
      originName: R.compose(R.prop("originName"), R.head)(jobs),
      origin: R.compose(R.prop("originUuid"), R.head)(jobs),
      specialization: "UNKNOWN_SPECIALIZATION",
    });
  }

  let obj = {
    status: result.status,
    originName: result.originName,
    origin: result.originUuid,
    specialization: R.trim(result.result.stdout),
  };
  return Promise.resolve(obj);
};

//generic: structure-specialize
const specialize = (input) => {
  const rest = new RestConnector(
    R.defaultTo("localhost", R.path(["api", "server"], input)),
    R.defaultTo(3000, R.path(["api", "port"], input)),
  );
  rest.setToken(input.token);

  let find = graphLib.findTypeByName(input.db);
  let getId = R.path(["meta", "uuid"]);

  let toEnt = find(R.trim(input.entry.specialization));
  let idIsNil = R.compose(R.isNil, getId);
  if (idIsNil(toEnt))
    return Promise.resolve({
      entity: input.entry.origin,
      status: "ERROR",
      message: "No specialization possible",
    });

  return rest
    .specializeInstance(input.entry.origin, getId(toEnt))
    .then((result) => {
      //console.log(result);
      return {
        entity: input.entry.origin,
        status: "SUCCESS",
        result: result,
      };
    })
    .catch((ex) => {
      ex.name = "ApiError";
      throw ex;
    });
};

module.exports = {
  getAllTypes: getAllTypes,
  nmap: R.curry(nmap),
  createFilter: R.curry(createFilter),
  guessByPort: R.curry(guessByPort),
  unixCreate: R.curry(unixCreate),
  windowsCreate: R.curry(windowsCreate),
  genericCreate: R.curry(genericCreate),
  deepDetect: R.curry(deepDetect),
  specialize: R.curry(specialize),
  handleStatusMessage: R.curry(handleStatusMessage),
};
