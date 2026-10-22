() => {
  return {
    db: aeonCache.get(aeonCache.get("Structure_allTypes_result_cacheEntry")),
    token: aeonCache.get("Structure_api_token"),
    host: aeonCache.get("Structure_api_host"),
    port: aeonCache.get("Structure_api_port"),
    existingInstance: dataDictionary[aeonCache.get("Structure_getInstance_result_dictEntry")],
    requestedType: dataDictionary.typeName,
    relation: "HAS",
    data: dataDictionary.instanceData,
  };
};
