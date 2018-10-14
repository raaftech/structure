/**
* Created by tushar-chauhan on 12-Apr-2015
*/

var DataSource = require('loopback-datasource-juggler').DataSource;
var db = new DataSource(require('../index'), {
  file_name: 'dev.sqlite3',
  debug: false
});

var model = {
  'name': {'type': String, 'required': true},
  'universe': {'type': String}
};

var Superhero = db.define('Superhero', model);

var heroes = [
  {name: 'Batman', universe: 'DC'},
  {name: 'The Hulk', universe: 'Marvel Comics'},
  {name: 'Green Arrow', universe: 'DC'},
  {name: 'Superman', universe: 'DC'},
  {name: 'Aquaman', universe: 'DC'},
];

db.isActual('Superhero',function(err, actual) {
  if (!actual) {
    // Update Model
    db.autoupdate(function(){
      // CREATE
      Superhero.create(heroes, function(err, h){
        if(err){
          console.log(err);
        } else {
          console.log("Created: \n", h);
          // FIND
          Superhero.find({where: {universe: 'DC'}}, function(err, arr){
            console.log("\nFind by universe 'DC' : \n", arr);
            // UPDATE
            Superhero.update({universe: 'DC'}, {universe: 'DC Comics'}, function(err, data){
              console.log("\nData after updating universe:");
              //Show All
              Superhero.all(function(err, data){
                console.log(data);
                console.log("\nData after deleting a row:");
                // DELETE
                Superhero.destroyAll({name: 'Aquaman'}, function(err, data){
                  //Show All after delete
                  Superhero.all(function(err, data){
                    console.log(data);
                  });
                });
              });
            });
          });
        }
      });
    });
  } else {
    console.log('No change in model definition.');
  }
});
