angular.module("structureApp")
    .controller("instrumentCtrl", function($scope, vertxEventBus, vertxEventBusService, instrumentSrvs) {

        $scope.getInstruments = function(uuid) {
            instrumentSrvs.getInstruments(uuid).then(function(response) {
                $scope.instruments = response.data;
            });
        };

        $scope.runInstrument = function(data) {
            instrumentSrvs.runInstrument(data).then(function(response) {
                // Nothing yet!
            });
        };
    });