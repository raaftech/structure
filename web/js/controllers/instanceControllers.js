angular.module("structureApp")
    .controller("instanceCtrl", function($scope, vertxEventBus, vertxEventBusService, instanceSrvs, demoSrvs) {

        $scope.getNInstances = function(n) {
            $scope.instances = '';
            instanceSrvs.getNInstances($scope, n);
        };

        $scope.getInstance = function(uuid) {
            instanceSrvs.getInstance(uuid).then(function(response) {
                $scope.inst = response;
            });
        };

        /**
         * Functionality for demo
         */

        $scope.showComponents = [];

        $scope.getRaafMachines = function() {
            instanceSrvs.getRaafMachines().then(function(response) {
                $scope.instances = response.data;
                //console.log(response.data);
            });
        };

        $scope.setShowComponents = function(instance) {
            if($scope.showComponents[instance.uuid]) {
                delete $scope.showComponents[instance.uuid];
            }
            else{
                $scope.showComponents[instance.uuid] = 1;
            }
        };

        $scope.createObjs = function() {
            demoSrvs.createObjs($scope);
        };

        $scope.returnInstance = function(uuid) {
            instanceSrvs.getRaafMachine({'uuid': uuid}).then(function(response) {
                $scope.instances.push(response);
            });
        };
    });