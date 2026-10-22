angular.module("structureApp")

    .directive("showIcon", function() {
        return {
            restrict: "E",
            controller: "iconCtrl",
            scope: {
                glyphid: '@',
                size: '@'
            },
            templateUrl: "js/views/directives/showIcon.html"
        }
    });