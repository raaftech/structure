angular.module("structureApp")
    .controller("iconCtrl", function($scope) {

        $scope.convertIcon = function(glyphid) {
            return String.fromCharCode(parseInt(glyphid, 16));
        };

    });
