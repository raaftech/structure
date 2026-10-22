angular.module("structureApp").controller("panelRightCtrl", function ($scope) {
  $scope.panelRight = false;
  $scope.panelRightSize = "50%";

  $scope.openPanelRight = function () {
    $scope.panelRight = true;
  };

  $scope.closePanelRight = function () {
    $scope.panelRight = false;
  };
});
