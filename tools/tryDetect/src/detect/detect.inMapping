() => {
    "use strict";
    let url = util.format('http://%s:%s/api/Instances/exec/Detect/on/%s?access_token=%s',
        cache.get("Structure_api_host"), cache.get("Structure_api_port"),
        options[cache.get("Structure_detect_instance_uuid_dictEntry")],
        cache.get("Structure_api_token"));

    let args = {
        capability: 'Detect',
        token: cache.get("Structure_api_token")
    };

    return fetch(url, {
        method: 'POST',
        body: 'token='+args.token,
        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
    })
        .then(result => result.json())
        .then(json => {
            options[cache.get("Structure_detect_result_dictEntry")] = json;
            return options;
        });
}