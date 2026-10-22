angular.module("structureApp").controller("searchCtrl", function ($scope, $location, searchSrvs) {
  $scope.query = "";

  $scope.startSearch = function () {
    searchSrvs.start($scope.query);
    $location.path("/search");
  };
});
