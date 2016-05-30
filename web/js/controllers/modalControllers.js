angular.module("structureApp")
    .controller("modalCtrl", function($scope, $uibModal) {

        $scope.open = function (id) {

            var modalInstance = $uibModal.open({
                animation: false,
                templateUrl: 'js/views/modalWindow.html',
                controller: 'modalInstanceCtrl',
                resolve: {
                    uuid: function () {
                        return id;
                    }
                }
            });
        };
    });