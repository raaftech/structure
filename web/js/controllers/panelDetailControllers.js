angular.module("structureApp")
    .controller("panelDetailCtrl", function($scope) {

        $scope.showDetails = false;

        $scope.detailedInfo = function() {
            $scope.showDetails = true;
        };

        $scope.closeDetailedInfo = function() {
            $scope.showDetails = false;
        };
    });