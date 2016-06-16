angular.module("structureApp", ["knalli.angular-vertxbus", "schemaForm", "ngRoute", "ui.bootstrap"])
    .config(function(vertxEventBusProvider, $routeProvider) {

        var locationArr = window.location.href.split("/")
        vertxEventBusProvider
            .enable()
            .useUrlServer(locationArr[0] + "//" + locationArr[2]);

        $routeProvider
            .when("/", {
                templateUrl: "js/views/home.html"
            })
            .when("/search", {
                templateUrl: "js/views/search.html"
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
            .when("/debug", {
                templateUrl: "js/views/debug.html"
            })
            .when("/task-manager", {
                templateUrl: "js/views/taskManager.html"
            })
            .when("/names-and-glyphs", {
                templateUrl: "js/views/namesAndGlyphs.html"
            })
            .when("/computers", {
                templateUrl: "js/views/computers.html"
            })
            .otherwise({
                templateUrl: "js/views/home.html"
            });
    });