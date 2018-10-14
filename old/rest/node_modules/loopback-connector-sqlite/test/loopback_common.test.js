/**
* Created by tushar-chauhan on 25-Mar-2015
*/

describe('SQLite imported features', function () {

  before(function () {
    require('./init.js');
  });

  require('loopback-datasource-juggler/test/common.batch.js');
  require('loopback-datasource-juggler/test/include.test.js');

});
