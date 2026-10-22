angular.module("structureApp")
    .controller("debugCtrl", function($scope, $timeout, vertxEventBus, vertxEventBusService) {

        // The address and the JSON text of the message to send
        $scope.message = { };

        $scope.sendMessage = function() {
            var json = JSON.parse($scope.message.unmarshalledText);
            vertxEventBusService.send($scope.message.address, json)
                .then(function(reply) {
                    console.log(reply);
                });
        }
    });
