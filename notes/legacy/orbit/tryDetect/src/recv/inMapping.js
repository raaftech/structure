(payload) => {
  aeonCache.set("Structure_api_token", payload.token);
  let obj = {
    token: payload.token,
    typeName: payload.typeName,
    instanceData: payload.data,
  };
  obj[aeonCache.get("Structure_getInstance_uuid_dictEntry")] = payload.instanceUuid;

  return obj;
};
