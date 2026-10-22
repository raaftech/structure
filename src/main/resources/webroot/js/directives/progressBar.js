angular.module("structureApp").directive("taskProgressBar", function () {
  return {
    restrict: "E",
    controller: "taskManagerCtrl",
    scope: {
      maxJobs: "@",
      doneJobs: "@",
      progressClass: "@",
      progressType: "@",
    },
    templateUrl: "js/views/directives/taskProgressBar.html",
  };
});
