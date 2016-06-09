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
        $scope.color = [];

        $scope.getRaafMachines = function() {
            instanceSrvs.getRaafMachines().then(function(response) {
                $scope.instances = response.data;
                instanceSrvs.liveStatus($scope);
                instanceSrvs.startChecking();
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
            instanceSrvs.startChecking();
        };

        $scope.logStatus = function(response) {
            if(response.event == 'serviceStatus') {
                if(!response.data.uuid in $scope.color) {
                    var obj = {};
                    obj[response.data.uuid] = response.data.status;
                    $scope.color.push(obj);
                }
                else {
                    $scope.color[response.data.uuid] = response.data.status;
                }
            }
        };

        $scope.checkColor = function(uuid) {
            return $scope.color[uuid];
        };
    });