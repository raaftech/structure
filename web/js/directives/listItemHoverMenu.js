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
    });
