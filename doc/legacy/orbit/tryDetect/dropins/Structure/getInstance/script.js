() => {
  "use strict";
  let url = util.format(
    "http://%s:%s/api/Instances/uuid/%s",
    cache.get("Structure_api_host"),
    cache.get("Structure_api_port"),
    options[cache.get("Structure_getInstance_uuid_dictEntry")],
  );

  return fetch(url)
    .then((result) => result.json())
    .then((json) => {
      options[cache.get("Structure_getInstance_result_dictEntry")] = json;
      return options;
    });
};
