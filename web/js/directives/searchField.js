angular.module("structureApp")
    .directive("searchField", function() {
        return {
            restrict: "E",
            scope: {},
            templateUrl: "js/directives/searchField.html"
        };
    });