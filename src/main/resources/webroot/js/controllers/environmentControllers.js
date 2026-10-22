angular.module("structureApp").controller("environmentCtrl", function ($scope, $controller, typeSrvs, instanceSrvs) {
  $controller("panelDetailCtrl", { $scope: $scope });
  $controller("panelRightCtrl", { $scope: $scope });
  $scope.showSize = "230px";

  $scope.schema = {
    type: "object",
    title: "Cascading select",
    properties: {
      isa: {
        type: "string",
        default: "",
      },
    },
  };

  $scope.form = [
    {
      key: "isa",
      notitle: true,
      type: "select",
    },
  ];

  $scope.model = {};
  $scope.types = [{ uuid: "a79ce509-a021-4177-ae61-73abd01fb479", name: "Component" }];
  $scope.list2 = [];
  $scope.isCollapsed = true;

  $scope.createIsaSelectBox = function (uuid) {
    typeSrvs.getIsaChildren(uuid).then(function (data) {
      $scope.stacked = data;
      $scope.list1 = data;
      $scope.form = [
        {
          key: "isa",
          notitle: true,
          type: "select",
          titleMap: data,
          onChange: "changeIsa(modelValue)",
        },
      ];
      $scope.$broadcast("schemaFormRedraw");
    });
  };

  $scope.changeIsa = function (uuid) {
    typeSrvs.getType(uuid).then(function (data) {
      $scope.types.push({
        uuid: uuid,
        name: data.name,
      });
      typeSrvs.getIsaChildren(uuid).then(function (data) {
        var dataObject = {
          key: "isa",
          notitle: true,
          type: "select",
        };
        if (data.length > 1) {
          $scope.stacked = data;
          $scope.list1 = data;
          dataObject.titleMap = data;
          dataObject.onChange = "changeIsa(modelValue)";
          $scope.form = [dataObject];
          $scope.$broadcast("schemaFormRedraw");
        }
      });
    });
  };

  $scope.revertIsa = function (args) {
    $scope.types.splice(args.index);
    $scope.changeIsa(args["uuid"]);
  };

  $scope.editTypeForm = {};

  $scope.removeSelected = function (node) {
    var index = $scope.list2.indexOf(node);
    $scope.list2.splice(index, 1);
  };

  $scope.node = {};
  $scope.node.schema = [];
  $scope.node.form = [];
  $scope.node.model = [];

  $scope.createForm = function (node) {
    $scope.node.schema[node.uuid] = node.schema;
    $scope.node.form[node.uuid] = ["*"];
    $scope.node.model[node.uuid] = [];
    angular.forEach(node, function (v, k) {
      $scope.node.model[node.uuid][k] = v;
    });
  };

  $scope.testSubmit = function () {
    console.log($scope.editTypeForm);
  };

  $scope.createIsaSelectBox("a79ce509-a021-4177-ae61-73abd01fb479");

  $scope.getNInstances = function (n) {
    instanceSrvs.getNInstances(n).then(function (response) {
      $scope.instances = response;
    });
  };

  $scope.getInstance = function (uuid) {
    instanceSrvs.getInstance(uuid).then(function (response) {
      $scope.instance = response;
    });
  };
});
