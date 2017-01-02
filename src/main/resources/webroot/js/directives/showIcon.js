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
    })

    .directive("nameAndGlyph", function() {
        return {
            restrict: "E",
            controller: "iconCtrl",
            scope: {
                name: '@',
                glyph: '@'
            },
            templateUrl: "js/views/directives/nameAndGlyph.html"
        }
    });