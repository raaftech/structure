(function() {
    var app = angular.module('universal', ['knalli.angular-vertxbus'])

    app.config(function(vertxEventBusProvider) {
        vertxEventBusProvider
        .enable()
        .useUrlServer('http://localhost:8080');
    });

    // UI controller, switching between tabs, that kinda stuff
    app.controller('ContainerController', function() {
        this.tab = 1;

        this.isSelected = function(checkTab) {
            return this.tab === checkTab;
        }

        this.changeTab = function(setTab) {
            this.tab = setTab;
        }
    });

    // TemplateController, controlling everything to do with templates
    app.controller('TemplateController', ['$scope', '$timeout', 'vertxEventBus', 'vertxEventBusService',
                                          function($scope, $timeout, vertxEventBus, vertxEventBusService) {
        // newTemplate is used to create the new TemplateNode via the eventbus
        $scope.newTemplate = { required: false, unique: false, start: false };
        // Data that's only used for the view
        $scope.ui = {};
        $scope.ui.databaseInitError = { checked: false, flag: false };
        $scope.ui.submitError = {};
        $scope.ui.submitSucceeded = {};
        $scope.ui.parentMenu = [];

        // This checks if the database is initialized and shows an db-init screen if not
        this.checkDatabaseInit = function() {
            // Did we check already? Is the eventbus ready?
            if ($scope.ui.databaseInitError.checked == false && vertxEventBus.readyState()) {
                // Query the database for the root-node
                vertxEventBusService.send('org.openpcf.universal.actor.framehelper',
                        {'msgh':'typenode', 'args':{'node':'b1bc963a-19cc-4518-aecb-a35d028c49e3'}})
                        .then(function(reply) {
                            // if we got an error we didn't find the root-node
                            if (reply.result.error != null) {
                                $timeout(function() {
                                    // Flag error as true so we see the db-init screen in the view
                                    $scope.ui.databaseInitError.flagged = true;
                                }, 0);
                            }
                        });
                $timeout(function() {
                    // Set checked to true, so we don't check again
                    $scope.ui.databaseInitError.checked = true;
                });
            }
        }

        // Initialize the database
        this.databaseInit = function() {
            if (vertxEventBus.readyState()) {
                // Send the db-init message over the eventbus
                vertxEventBusService.send('org.openpcf.universal.actor.schemamanager',
                        {'msgh':'update', 'args':{}})
                        .then(function(reply) {
                            // If everything went as expected clear the error flag
                            if (reply.result.error == null) {
                                $timeout(function() {
                                    $scope.ui.databaseInitErrorClear();
                                }, 0);
                            } else {
                                alert(reply.result.error);
                            }
                        });
            }
        }

        // Clear the error flag so the db-init screen disappears from the view
        $scope.ui.databaseInitErrorClear = function() {
            $timeout(function() {
                $scope.ui.databaseInitError.flagged = false;
            }, 0);
        };


        // Create a new TemplateNode
        this.createNewTemplate = function() {
            if (vertxEventBus.readyState()) {
                // Send our newTemplate object over the eventbus
                vertxEventBusService.send('org.openpcf.universal.actor.framehelper',
                        {'msgh':'createtemplatenode', 'args':$scope.newTemplate})
                        .then(function(reply) {
                            // If everything went alright clear the form and show an informational message
                            if (reply.result.error == null) {
                                $timeout(function() {
                                    $scope.newTemplate = {};
                                    $scope.newTemplateForm.$setPristine(true);
                                    $scope.ui.submitError.flag = false;
                                    $scope.ui.submitSucceeded.flag = true;
                                    $scope.ui.submitSucceeded.message = 'New node has been created with UUID: ' + reply.result.uuid;
                                }, 0);
                            } else {
                                // Else we'll show an error message
                                $timeout(function() {
                                    $scope.ui.submitSucceeded.flag = false;
                                    $scope.ui.submitError.flag = true;
                                    $scope.ui.submitError.message = reply.result.error;
                                }, 0);
                            }
                        }).catch(function() {
                            $timeout(function() {
                                $scope.ui.submitSucceeded.flag = false;
                                $scope.ui.submitError.flag = true;
                                $scope.ui.submitError.message = 'Error while communicating with eventbus';
                            }, 0);
                        });
            }
        };

        // Build object for the dropdown for parent selection
        this.getTemplates = function() {
            if (vertxEventBus.readyState()) {
                vertxEventBusService.send('org.openpcf.universal.actor.framehelper',
                        {'msgh':'findalltemplatenodes', 'args':{}})
                        .then(function(reply) {
                            $scope.ui.parentMenu = [];
                            // Inner recursion function to recurse over subtemplates
                            function recurse(template, count) {
                                var indent = '';
                                for (i = 0; i < count; i++) {
                                    indent = indent + ' ';
                                };
                                indent = indent + '└ ';
                                $scope.ui.parentMenu.push({'name': indent + template.name, 'uuid': template.uuid});
                                // Iterate over children
                                if (template.children != null) {
                                    for (i in template.children) {
                                        recurse(template.children[i], count+1);
                                    }
                                }
                            }
                            // Iterate over found templates
                            for (i in reply.result.templates) {
                                node = reply.result.templates[i];
                                $scope.ui.parentMenu.push({'name': node.name, 'uuid': node.uuid});
                                // Recurse over children
                                if (node.children != null) {
                                    for (i in node.children) {
                                        recurse(node.children[i], 1);
                                    }
                                }
                            }
                        }).catch(function() {
                        });
            }
        };

        // Find a TypeNode either by name or by UUID
        this.findTypeNode = function() {
            if (vertxEventBus.readyState()) {
                vertxEventBusService.send('org.openpcf.universal.actor.framehelper',
                        {'msgh':'typenode', 'args':{'node': $scope.newTemplate.isa}})
                        .then(function(reply) {
                            console.log(reply);
                            if (reply.result.error == null) {
                                $timeout(function() {
                                    $scope.newTemplate.isa = reply.result.uuid;
                                });
                            }
                        }).catch(function() {
                            console.warn(reply);
                        });
            }
        };

        // When we click the required checkbox also check required and unique
        this.clickStart = function() {
            $scope.newTemplate.required = true;
            $scope.newTemplate.unique = true;
        }
    }]);
})();