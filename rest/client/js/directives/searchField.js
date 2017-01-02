angular.module("structureApp")
    .directive("searchField", function() {
        return {
            restrict: "E",
            scope: {},
            templateUrl: "js/views/directives/searchField.html"
        };
    });