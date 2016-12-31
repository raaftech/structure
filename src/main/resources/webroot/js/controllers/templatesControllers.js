angular.module("structureApp").
    controller("templateCtrl", function($scope, vertxEventBus, vertxEventBusService, templateSrvs) {

        $scope.getAllTemplates = function(uuid) {
            // reset (sub)templates
            $scope.templates = '';
            $scope.subTemplates = [];
            // get data
            if(uuid) {
                templateSrvs.getTemplate($scope, uuid);
                templateSrvs.getTemplates($scope, uuid);
            }
            else{
                templateSrvs.getAllTemplates($scope);
            }
        };

        $scope.getSubTemplates = function(uuid) {
            if($scope.subTemplates[uuid]) {
                delete $scope.subTemplates[uuid];
            }
            else{
                templateSrvs.getSubTemplates($scope, uuid);
            }
        }
    });