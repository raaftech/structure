angular.module("structureApp",
    [
        "schemaForm",
        "ngRoute",
        "ui.bootstrap",
        "hSweetAlert",
        "pageslide-directive",
        "lbServices"
    ])
    .config(function($routeProvider, $locationProvider) {
        $routeProvider
            .when("/", {
                templateUrl: "js/views/home.html"
            })
            .when("/debug", {
                templateUrl: "js/views/debug.html"
            })
            .when("/environment", {
                templateUrl: "js/views/environment.html"
            })
            .when("/names-and-glyphs", {
                templateUrl: "js/views/namesAndGlyphs.html"
            })
            .when("/search", {
                templateUrl: "js/views/search.html"
            })
            .when("/task-manager", {
                templateUrl: "js/views/taskManager.html"
            })
            .when("/templates", {
                templateUrl: "js/views/templates.html"
            })
            .when("/templates-old", {
                templateUrl: "js/views/templatesOld.html"
            })
            .when("/types", {
                templateUrl: "js/views/types.html"
            })
            .otherwise({
                templateUrl: "js/views/home.html"
            });

        $locationProvider.html5Mode({
            enabled: false
        });
    });
