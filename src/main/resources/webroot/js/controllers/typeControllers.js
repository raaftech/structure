angular.module("structureApp")
    .controller("typeCtrl", function($scope, $uibModal, $controller, typeSrvs, sweet) {

        $controller("panelDetailCtrl", {$scope: $scope});
        $controller("panelRightCtrl", {$scope: $scope});

        $scope.getType = function(uuid) {
            typeSrvs.getType(uuid).then(function(response) {
                $scope.type = response;
                typeSrvs.getType($scope.type.isa).then(function(response) {
                    $scope.isa = response;
                });
            });
        };

        $scope.getChildren = function(node) {
            if(node.nodes && node.nodes.length > 0) {
                node.nodes = [];
            }
            else {
                typeSrvs.getChildren(node.uuid).then(function (response) {
                    node.nodes = response;
                });
            }
        };

        $scope.addType = function(uuid) {
            typeSrvs.getType(uuid).then(function(response) {
                typeSrvs.getType(response.isa).then(function(response) {
                    $scope.schema = response.schema;
                    $scope.model = {
                        "isa": response.uuid
                    };
                    $scope.form = [
                        "*",
                        {
                            "type": "actions",
                            "items": [
                                {
                                    "type": "submit",
                                    "title": "Add"
                                },
                                {
                                    "type": "button",
                                    "title": "Cancel",
                                    "onClick": "cancel()"
                                }
                            ]
                        }];
                    $scope.createIsaSelectBox(response.isa);
                });
                $uibModal.open({
                    "controller": "modalEditTypeCtrl",
                    "templateUrl": "js/views/modals/schemaFormAddType.html",
                    "scope": $scope
                });
            });
        };

        $scope.editType = function(uuid) {
            typeSrvs.getType(uuid).then(function(response) {
                $scope.type = response;
                $scope.schema = $scope.type.schema;
                $scope.model = {};
                $scope.form = [
                    "*",
                    {
                        "type": "actions",
                        "items": [
                            {
                                "type": "submit",
                                "title": "Ok"
                            },
                            {
                                "type": "button",
                                "title": "Cancel",
                                "onClick": "closePanelRight()"
                            }
                        ]
                    }];
                $scope.form.unshift(
                    {
                        "key": "uuid",
                        "title": "uuid",
                        "type": "text"
                    });
                typeSrvs.getType($scope.type.isa).then(function(response) {
                    $scope.isa = response;
                    $scope.createIsaSelectBox($scope.isa.isa);
                });
                angular.forEach($scope.type, function(v, k) {
                    $scope.model[k] = v;
                });
                // $uibModal.open({
                //     "controller": "modalEditTypeCtrl",
                //     "templateUrl": "js/views/modals/schemaFormEditType.html",
                //     "scope": $scope
                // });
            });
        };

        $scope.removeType = function(uuid) {
            typeSrvs.getType(uuid).then(function(response) {
                sweet.show({
                    "title": "Confirm",
                    "text": "Are you sure you want to remove type '" + response.name + "'?",
                    "type": "warning",
                    "showCancelButton": true,
                    "confirmButtonText": "Yes, delete it!",
                    "closeOnConfirm": false
                }, function() {
                    sweet.show("Deleted!", "Type '" + response.name + "' has been removed!", "success");
                });
            });
        };

        $scope.createIsaSelectBox = function(uuid) {
            typeSrvs.getIsaChildren(uuid).then(function(data) {
                $scope.form.unshift(
                    {
                        "key": "isa",
                        "title": "isa",
                        "type": "select",
                        "titleMap": data
                    });
                $scope.$broadcast("schemaFormRedraw");
            });
        };

        $scope.createInstance = function(uuid) {
            typeSrvs.getType(uuid).then(function(response) {
                $scope.instance = response;
                $scope.schema = $scope.instance.schema;
                $scope.model = {
                    "node": uuid
                };
                $scope.form = [
                    "*",
                    {
                        "type": "actions",
                        "items": [
                            {
                                "type": "submit",
                                "title": "Ok"
                            },
                            {
                                "type": "button",
                                "title": "Cancel",
                                "onClick": "cancel()"
                            }
                        ]
                    }];
                $uibModal.open({
                    "controller": "modalInstanceCtrl",
                    "templateUrl": "js/views/modals/schemaFormCreateInstance.html",
                    "scope": $scope
                });
            });
        };
        
        $scope.init = function(uuid) {
            $scope.getType(uuid);
            typeSrvs.getChildren(uuid).then(function(response) {
                $scope.nodes = response;
            });
            $scope.showDetails = true;
            $scope.showSize = "230px";
        };

        $scope.init("a79ce509-a021-4177-ae61-73abd01fb479");
    });