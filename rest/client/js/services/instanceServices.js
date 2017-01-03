angular.module("structureApp")
    .factory("instanceSrvs", function(Entity) {

        return {
            getNInstances: function(n) {
                console.log('getNInstances(%s) called', n);
                return new Promise((resolve, reject) => {
                  Entity.lastCreated({quantity: n}, function(result) {
                      console.log(result);
                      console.log(result.result);
                    resolve(result.result);
                  });
                });
            },

            getInstance: function(uuid) {
                return new Promise((resolve, reject) => {
                  Entity.fetchInstance({uuid: uuid}, function(instance) {
                    resolve(instance.result);
                  });
                });
            },

            putNode: function(args) {
                return new Promise((resolve, reject) => {
                  Entity.createInstance({args: args}, function(id) {
                    resolve(id.result);
                  });
                });
            }
        }
    });
