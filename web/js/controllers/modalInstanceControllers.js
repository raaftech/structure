angular.module("structureApp")
    .controller("modalInstanceCtrl", function($scope, $uibModalInstance, uuid, vertxEventBus, vertxEventBusService) {

        $scope.uuid = uuid;

        $scope.getType = function(uuid) {
            vertxEventBusService.send('com.raaftech.structure.repository.typenodes',
                {
                    'msgh': 'findOne',
                    'args': {
                        'node': uuid
                    }
                })
                .then(function(reply) {
                    $scope.type = reply.result.data;

                    $scope.schema = $scope.type.schema;
                    $scope.form = [
                        "*"
                    ];
                    $scope.model = {
                        "node": uuid
                    };
                });
        };

        $scope.ok = function() {
            $uibModalInstance.close();
        };

        $scope.cancel = function() {
            $uibModalInstance.dismiss('cancel');
        };

        $scope.showFormValues = function() {
            var formArgs = {};
            for (var k in $scope.model){
                if ($scope.model.hasOwnProperty(k)) {
                    formArgs[k] = $scope.model[k];
                }
            }

            vertxEventBusService.send('com.raaftech.structure.repository.instancenodes',
                {
                    'msgh': 'putNode',
                    'args': formArgs
                })
                .then(function(reply) {
                    if(reply.result.error) {
                        console.log(reply.result.error);
                    }
                    else {
                        console.log(reply.result.data);
                    }
                    $scope.result = reply.result;
                });
        }
    });