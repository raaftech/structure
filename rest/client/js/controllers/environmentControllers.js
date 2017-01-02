angular.module("structureApp")
    .controller("environmentCtrl", function($scope, typeSrvs) {

        $scope.schema = {
            "type": "object",
            "title": "Cascading select",
            "properties": {
                "isa": {
                    "type": "string",
                    "default": ""
                }
            }
        };

        $scope.form = [{
            "key": "isa",
            "notitle": true,
            "type": "select"
        }];

        $scope.model = {};
        $scope.types = [{"uuid": "a79ce509-a021-4177-ae61-73abd01fb479", "name": "Component"}];

        $scope.createIsaSelectBox = function(uuid) {
            typeSrvs.getIsaChildren(uuid).then(function(data) {
                $scope.stacked = data;
                $scope.form = [{
                    "key": "isa",
                    "notitle": true,
                    "type": "select",
                    "titleMap": data,
                    "onChange": "changeIsa(modelValue)"
                }];
                $scope.$broadcast("schemaFormRedraw");
            });
        };

        $scope.changeIsa = function(uuid) {
            typeSrvs.getType(uuid).then(function(data) {
                $scope.types.push({
                    "uuid": uuid,
                    "name": data.name
                });
                typeSrvs.getIsaChildren(uuid).then(function(data) {
                    var dataObject = {
                        "key": "isa",
                        "notitle": true,
                        "type": "select"
                    };
                    if(data.length > 1) {
                        $scope.stacked = data;
                        dataObject.titleMap = data;
                        dataObject.onChange = "changeIsa(modelValue)";
                        $scope.form = [dataObject];
                        $scope.$broadcast("schemaFormRedraw");
                    }
                });
            });
        };

        $scope.revertIsa = function(args) {
            $scope.types.splice(args.index);
            $scope.changeIsa(args['uuid']);
        };

        $scope.createIsaSelectBox("a79ce509-a021-4177-ae61-73abd01fb479");
    });