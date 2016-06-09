angular.module("structureApp")
    .controller("instanceCtrl", function($scope, vertxEventBus, vertxEventBusService, instanceSrvs) {

        $scope.getNInstances = function(n) {
            $scope.instances = '';
            instanceSrvs.getNInstances($scope, n);
        };
    });