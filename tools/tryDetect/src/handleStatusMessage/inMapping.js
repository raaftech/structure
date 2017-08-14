(payload) => {
    let jobs = payload.data.taskObj.jobs;
    let result = R.find(job => {
        if(R.isNil(job.result)) return false;
        let str = R.trim(job.result.stdout);
        return (str !== "" && str !== "\n");
    },jobs);

    let obj = {};
    obj.detectOutputObject = result;

    if(R.isNil(result)) {
        obj[aeonCache.get("Structure_specialize_specialization_dictEntry")] = "";
		dataDictionary.jobStatus = 'FAILED';
		obj.jobStatus = 'FAILED';
        return obj;
    } else {
		dataDictionary.jobStatus = R.defaultTo('FAILED', result.status);
		obj.jobStatus = R.defaultTo('FAILED', result.status);
	}

    let found = R.find(R.propEq('name', result.result.stdout), aeonCache.get(aeonCache.get("Structure_allTypes_result_cacheEntry")));
    if(R.isNil(found)) {
        obj[aeonCache.get("Structure_specialize_specialization_dictEntry")] = "";
    } else {
        obj[aeonCache.get("Structure_specialize_specialization_dictEntry")] = found.meta.uuid;
    }
    
    return obj;
}