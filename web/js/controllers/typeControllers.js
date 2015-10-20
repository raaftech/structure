angular.module("structureApp")
    .controller("typeCtrl", function($scope, vertxEventBus, vertxEventBusService, typeSrvs) {

        $scope.getType = function(uuid) {
            // reset (sub)types
            $scope.types = '';
            $scope.subtypes = [];
            // get data
            typeSrvs.getType($scope, uuid);
            typeSrvs.getTypes($scope, uuid);
        };

        $scope.getSubtypes = function(type) {
            if($scope.subtypes[type.uuid]) {
                delete $scope.subtypes[type.uuid];
            }
            else{
                typeSrvs.getSubtypes($scope, type.uuid);
            }
        };
    });