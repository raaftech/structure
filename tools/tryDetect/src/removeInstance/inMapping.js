() => {
    "use strict";
    let url = util.format('http://%s:%s/api/Instances/%s?access_token=%s',
        cache.get("Structure_api_host"), cache.get("Structure_api_port"),
        options[cache.get("Structure_removeInstance_uuid_dictEntry")],
        cache.get("Structure_api_token"));

    return fetch(url, {method:"DELETE"})
        .then(result => result.json())
        .then(json => {
            options[cache.get("Structure_removeInstance_result_dictEntry")] = json;
            return options;
        });
}