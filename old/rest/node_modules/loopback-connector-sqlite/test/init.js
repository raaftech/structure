/**
* Created by tushar-chauhan on 13-Mar-2015
*/

module.exports = require('should');
var DataSource = require('loopback-datasource-juggler').DataSource;

var config = require('rc')('loopback').sqlite;

console.log(config);

global.getDataSource = global.getSchema = function (customConfig) {
 var db = new DataSource(require('../'), customConfig || config.test);

  db.log = function (msg) {
   console.log(msg);
  };

 return db;
};
