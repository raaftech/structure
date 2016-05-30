angular.module("structureApp")
    .controller("navigationCtrl", function($scope, $location) {

        $scope.navClass = function(page) {
            var current = $location.path().substring(1);
            return page === current ? "active" : "";
        };
    });