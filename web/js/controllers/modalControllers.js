angular.module("structureApp")
    .controller("modalCtrl", function($scope, $modal) {

        $scope.open = function (id) {

            var modalInstance = $modal.open({
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