/**
* Created by tushar-chauhan on 13-Mar-2015
*/

require('./init');
var expect = require('chai').expect;

var User, db;

describe("SQLite Model creation test", function(){

  before('Create connection and User model definition', function () {
    db = getDataSource();

    User = db.define('User', {
      id: {type: Number, id: true},
      name: { type: String },
      email: { type: String }
    });

  });

  after('Delete all users', function(done){
    User.destroyAll(function(err, data){
      if(err !== null){
        console.log(err);
      }
      db.connector.disconnect();
      done();
    });
  });

  it('should run auto-migration', function (done) {
    db.automigrate('User', function () {
      done();
    });
  });

  it('should create users', function(done){
    var users = [{name: "John", email: "john@test.com"}, {name: "Doe", email: "doe@test.com"}];

    for(var i=0; i<users.length; i++) {
      User.create(users[i], function(err, u) {
        expect(err).to.not.be.undefined;
        expect(err).to.be.null;
        if(u.id == 1)
          expect(u.name).to.be.equal("John");
        else if(u.id == 2) {
          expect(u.name).to.be.equal("Doe");
          testArrayContents(done)
          // done();
        }
      });
    }

    var searchCond = {'order': 'id'};
    function testArrayContents(cb) {
      var namesSomething = [{name: 'John', some: 'object'}, {name: 'Doe', some: 'other object'}];
      var names = [];
      namesSomething.forEach(function(n) {
        names.push(n.name);
      });
      searchCond.where = {name: {inq: names}};
      console.log('find:')
      User.find(searchCond, function(err, foundUsers) {
        expect(names[0]).to.be.equal('John');
        expect(names[1]).to.be.equal('Doe');
        User.find(searchCond, function(err, foundUsers) {
          expect(searchCond.order).to.be.equal('id');
          cb();
        });
      });
    }
  });

  it('should run auto-migration when model changes', function (done) {
    User = db.define('User', {
      id: {type: Number, id: true},
      new_name: { type: String }
    });
    db.automigrate('User', function () {
      done();
    });
  });

});
