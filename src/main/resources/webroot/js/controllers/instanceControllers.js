angular.module("structureApp").controller("instanceCtrl", function ($scope, instanceSrvs) {
  $scope.getNInstances = function (n) {
    instanceSrvs.getNInstances(n, ["Component", "Group"]).then(function (response) {
      $scope.instances = response;
    });
  };

  $scope.getInstance = function (uuid) {
    instanceSrvs.getInstance(uuid).then(function (response) {
      $scope.inst = response;
    });
  };
});
