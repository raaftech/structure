angular.module("structureApp")
    .controller("searchResultCtrl", function($scope, searchSrvs) {

        $scope.data = searchSrvs.data;
    });