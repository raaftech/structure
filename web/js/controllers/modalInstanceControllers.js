angular.module("structureApp")
    .controller("modalInstanceCtrl", function($scope, $uibModalInstance, instanceSrvs) {

        $scope.cancel = function() {
            $uibModalInstance.dismiss("cancel");
        };

        $scope.showFormValues = function(formData) {
            if(formData.$valid) {
                var formArgs = {};
                for(var k in $scope.model){
                    if($scope.model.hasOwnProperty(k)) {
                        formArgs[k] = $scope.model[k];
                    }
                }
                instanceSrvs.putNode(formArgs).then(function(response) {
                    $scope.result = response.result;
                });
            }
        }
    });