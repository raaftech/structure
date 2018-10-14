/**
* Created by tushar-chauhan on 13-Mar-2015
*/

require('./init');
var expect = require('chai').expect;

describe("SQLite3 connection test", function(){
  it('should connect to sqlite3 DB', function(done){
    var ds = getDataSource();
    ds.ping(function(err) {
      expect(err).to.not.be.undefined;
      expect(err).to.be.null;
      ds.connector.disconnect();
      done();
    });
  });
});
