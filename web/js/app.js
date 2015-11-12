var eventBus = new vertx.EventBus("http://localhost:8080/eventbus");

angular.module("structureApp", ["knalli.angular-vertxbus", "schemaForm", "ngRoute", "ui.bootstrap"])
    .config(function(vertxEventBusProvider, $routeProvider) {

        vertxEventBusProvider
            .enable()
            .useUrlServer("http://localhost:8080");

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
            .when("/types", {
                templateUrl: "js/views/types.html"
            })
            .when("/debug", {
                templateUrl: "js/views/debug.html"
            })
            .otherwise({
                templateUrl: "js/views/home.html"
            });
    });