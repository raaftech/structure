angular.module("structureApp")
    .factory("typeSrvs", function(Entity) {

        return {
            getType: function(uuid) {
                return new Promise((resolve, reject) => {
                  Entity.fetchByUuid({uuid: uuid}, function(entity) {
                    resolve(entity.result);
                  });
                });
            },

            getChildren: function(uuid) {
                return new Promise((resolve, reject) => {
                  Entity.getChildren({uuid: uuid}, function(children) {
                    resolve(children.result);
                  });
                });
            },

            getIsaChildren: function(uuid) {
                return new Promise((resolve, reject) => {
                  Entity.getChildren({uuid: uuid}, function(children) {
                    var titleMap = [];
                    angular.forEach(children.result, function(node) {
                      titleMap.push({
                        "value": node.uuid,
                        "name": node.name
                      });
                    });
                    resolve(titleMap);
                  });
                });
            }
        }
    });
