angular.module("structureApp")
    .directive("listItemHoverMenu", function() {
       return {
           restrict: "E",
           controller: "modalCtrl",
           scope: {
               uuid: '@'
           },
           templateUrl: "js/directives/listItemHoverMenu.html"
       };
    });