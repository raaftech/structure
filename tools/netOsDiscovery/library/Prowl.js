"use strict";

const util = require("util");
const R = require("ramda");
const fetch = require("node-fetch");

//apiKey can be a comma separated list of keys
const notify = (apiKey, appName, subject, message, url) => {
  return new Promise((resolve, reject) => {
    const prowlUrl = "https://api.prowlapp.com/publicapi/add";
    const _appName = encodeURIComponent(appName);
    const _subject = encodeURIComponent(subject);
    const msg = encodeURIComponent(message);
    let postData = util.format("apikey=%s&application=%s&event=%s&description=%s", apiKey, _appName, _subject, msg);
    if (!R.isNil(url) && url !== "") postData = util.format("%s&url=%s", postData, encodeURIComponent(url));

    //console.log('%s , %s', prowlUrl, postData);
    fetch(prowlUrl, {
      method: "POST",
      body: postData,
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
    })
      .then((result) => {
        if (R.isNil(result)) resolve({ code: 500, message: "No result received" });
        else
          resolve({
            code: result.status,
            message: result.statusText,
          });
      })
      .catch((ex) => reject(ex));
  });
};

module.exports = {
  notify: notify,
};
