angular.module("structureApp")
    .controller("debugCtrl", function($scope, $timeout, vertxEventBus, vertxEventBusService) {

        // newTemplate is used to create the new Template via the eventbus
        $scope.message = { };

        $scope.sendMessage = function() {
            var json = JSON.parse($scope.message.unmarshalledText);
            vertxEventBusService.send($scope.message.address, json, 5000)
                .then(function(reply) {
                    console.log(reply);
                });
        }
    });
