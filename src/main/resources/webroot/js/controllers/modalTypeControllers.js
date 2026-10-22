angular.module("structureApp").controller("modalEditTypeCtrl", function ($scope, $uibModalInstance) {
  $scope.saveType = function (formData) {
    console.log(formData);
  };

  $scope.addType = function (formData) {
    console.log(formData);
  };

  $scope.cancel = function () {
    $uibModalInstance.dismiss();
  };
});
