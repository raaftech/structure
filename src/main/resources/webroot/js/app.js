angular
  .module("structureApp", [
    "knalli.angular-vertxbus",
    "schemaForm",
    "ngRoute",
    "ui.bootstrap",
    "hSweetAlert",
    "pageslide-directive",
    "ngDragDrop",
    "ngAnimate",
  ])
  .config(function (vertxEventBusProvider, $routeProvider, $locationProvider) {
    var locationArr = window.location.href.split("/");
    vertxEventBusProvider.enable().useUrlServer(locationArr[0] + "//" + locationArr[2]);

    $routeProvider
      .when("/", {
        templateUrl: "js/views/home.html",
      })
      .when("/debug", {
        templateUrl: "js/views/debug.html",
      })
      .when("/environment", {
        templateUrl: "js/views/environment.html",
      })
      .when("/search", {
        templateUrl: "js/views/search.html",
      })
      .when("/task-manager", {
        templateUrl: "js/views/taskManager.html",
      })
      .when("/types", {
        templateUrl: "js/views/types.html",
      })
      .otherwise({
        templateUrl: "js/views/home.html",
      });

    $locationProvider.html5Mode({
      enabled: false,
    });
  });
