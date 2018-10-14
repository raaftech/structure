angular.module("structureApp")
    .controller("iconCtrl", function($scope, iconSrvs) {

        $scope.convertIcon = function(glyphid) {
            return String.fromCharCode(parseInt(glyphid, 16));
        };

        $scope.getNamesAndGlyphs = function() {
            iconSrvs.getNamesAndGlyphs().then(function(response) {
                $scope.namesAndGlyphs = response;
            });
        };

    });
