() => {
    "use strict";
    let url = util.format('http://%s:%s/api/Instances/specialize?access_token=%s',
        cache.get("Structure_api_host"), cache.get("Structure_api_port"),
        cache.get("Structure_api_token"));

    let instanceUuid = options[cache.get("Structure_specialize_origin_uuid_dictEntry")];
    let isaUuid = options[cache.get("Structure_specialize_specialization_dictEntry")];
    return fetch(url, {
        method: 'PUT',
        body: util.format('instanceUuid=%s&isaUuid=%s', instanceUuid, isaUuid),
        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
    })
        .then(result => result.json())
        .then(json => {
            options[cache.get("Structure_specialize_result_dictEntry")] = json;
            return options;
        });
}