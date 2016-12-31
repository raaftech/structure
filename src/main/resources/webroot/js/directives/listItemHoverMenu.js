angular.module("structureApp")

    .directive("listItemHoverMenu", function() {
        return {
            "restrict": "E",
            "controller": "modalCtrl",
            "scope": {
                "uuid": "@",
                "nodeType": "@"
            },
            "templateUrl": "js/views/directives/listItemHoverMenu.html"
        }
    })

    .directive("listItemHoverMenuTasks", function() {
        return {
            "restrict": "E",
            "controller": "taskManagerCtrl",
            "scope": {
                taskId: "@"
            },
            "templateUrl": "js/views/directives/listItemHoverMenuTasks.html"
        }
    })

    .directive("listItemHoverMenuJobs", function() {
        return {
            "restrict": "E",
            "controller": "taskManagerCtrl",
            "scope": {
                "taskId": "@",
                "jobId": "@"
            },
            "templateUrl": "js/views/directives/listItemHoverMenuJobs.html"
        }
    })

    .directive("listItemHoverMenuInstruments", function() {
        return {
            "restrict": "E",
            "controller": "instrumentCtrl",
            "scope": {
                "uuid": "@"
            },
            "templateUrl": "js/views/directives/listItemHoverMenuInstruments.html"
        }
    })

    .directive("listItemHoverMenuTypes", function() {
        return {
            "restrict": "E",
            "scope": true,
            "link": function(scope, element, attr) {
                scope.addTypeClicked = function() {
                    scope.$watch(
                        scope.addType(attr.uuid)
                    );
                };
                scope.createInstanceClicked = function() {
                    scope.$watch(
                        scope.createInstance(attr.uuid)
                    );
                };
                scope.editTypeClicked = function() {
                    scope.$watch(
                        scope.editType(attr.uuid),
                        scope.openPanelRight()
                    );
                };
                scope.removeTypeClicked = function() {
                    scope.$watch(
                        scope.removeType(attr.uuid)
                    );
                };
            },
            "templateUrl": "js/views/directives/listItemHoverMenuTypes.html"
        }
    });
