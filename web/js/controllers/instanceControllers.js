angular.module("structureApp")
    .controller("instanceCtrl", function($scope, vertxEventBus, vertxEventBusService, instanceSrvs) {

        $scope.getNInstances = function(n) {
            $scope.instances = '';
            instanceSrvs.getNInstances($scope, n);
        };

        $scope.getInstance = function(uuid) {
            instanceSrvs.getInstance(uuid).then(function(response) {
                $scope.inst = response;
            });
        };
    });