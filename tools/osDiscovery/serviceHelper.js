"use strict";

const R = require("ramda");

const notNil = R.compose(R.not, R.isNil);

const getServiceProperty = (service) =>
  R.pipe(
    R.prop("openPorts"),
    R.filter((item) => !R.isNil(item)),
    R.find(R.propEq("service", service)),
  );

/**
 * Function that generates a function that can check objects, as returned by the nmapLib.nmapScan
 * function, for presence of a given service.
 * @param {String} service Name of the service to search for.
 * @returns {Boolean} Boolean describing if the service was found or not.
 **/
const hasService = (service) => R.pipe(getServiceProperty(service), notNil);

/**
 * Function that generates a function that can check objects, as returned by the nmapLib.nmapScan
 * function, for presence of a given service and if present, returns the service object.
 * @param {String} service Name of the service to search for.
 * @returns {String | Object} An object containing the service details or the string "none"
 **/
const getService = (service) => R.ifElse(hasService(service), getServiceProperty(service), R.always("none"));

module.exports = {
  hasService: hasService,
  getService: getService,
};
