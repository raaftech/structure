angular.module("structureApp")
    .factory("iconSrvs", function(Entity) {

        return {

            getNamesAndGlyphs: function() {
                return Promise.resolve({});
            }
        }
    });
