angular.module("structureApp")

    .directive("listItemHoverMenu", function() {
        return {
            restrict: "E",
            controller: "modalCtrl",
            scope: {
                uuid: '@',
                nodeType: '@'
            },
            templateUrl: "js/views/directives/listItemHoverMenu.html"
        }
    })

    .directive("listItemHoverMenuTasks", function() {
        return {
            restrict: "E",
            controller: "taskManagerCtrl",
            scope: {
                taskId: '@'
            },
            templateUrl: "js/views/directives/listItemHoverMenuTasks.html"
        }
    })

    .directive("listItemHoverMenuJobs", function() {
        return {
            restrict: "E",
            controller: "taskManagerCtrl",
            scope: {
                taskId: '@',
                jobId: '@'
            },
            templateUrl: "js/views/directives/listItemHoverMenuJobs.html"
        }
    })

    .directive("listItemHoverMenuInstruments", function() {
        return {
            restrict: "E",
            controller: "instrumentCtrl",
            scope: {
                uuid: '@'
            },
            templateUrl: "js/views/directives/listItemHoverMenuInstruments.html"
        }
    });
