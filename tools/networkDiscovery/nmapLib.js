"use strict";

const R = require("ramda");
const nmap = require("node-nmap");

const createErrorMessage = (error) => {
  return {
    error: error,
  };
};
const sanitize = R.project(["hostname", "ip", "openPorts"]);

const quickScan = (cidr) => {
  return new Promise((resolve, reject) => {
    nmap.nodenmap.nmapLocation = "nmap";

    let quickscan = new nmap.nodenmap.QuickScan(cidr);
    quickscan.on("complete", function (data) {
      resolve(data);
    });

    quickscan.on("error", function (error) {
      reject(createErrorMessage(error));
    });

    quickscan.startScan();
  });
};

const nmapScan = (cidr, args) => {
  args = R.defaultTo("", args);
  return new Promise((resolve, reject) => {
    nmap.nodenmap.nmapLocation = "nmap";

    let quickscan = new nmap.nodenmap.NmapScan(cidr, args);
    quickscan.on("complete", function (data) {
      resolve(sanitize(data));
    });

    quickscan.on("error", function (error) {
      reject(createErrorMessage(error));
    });

    quickscan.startScan();
  });
};

module.exports = {
  quickScan: quickScan,
  nmapScan: nmapScan,
};
