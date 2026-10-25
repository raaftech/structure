() => {
  "use strict";
  let url = util.format("http://%s:%s/api/Entities", cache.get("Structure_api_host"), cache.get("Structure_api_port"));
  return fetch(url)
    .then((result) => result.json())
    .then((json) => {
      cache.set(cache.get("Structure_allTypes_result_cacheEntry"), json);
      return options;
    });
};
