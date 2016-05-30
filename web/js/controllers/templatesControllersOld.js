angular.module("structureApp")
    .filter('epochtohuman', function() {
        return function(input) {
            var date = new Date(parseInt(input));
            return date.toLocaleString();
        }
    })

    .controller("templateCtrlOld", function($scope, $timeout, vertxEventBus, vertxEventBusService) {

        // newTemplate is used to create the new TemplateNode via the eventbus
        $scope.newTemplate = { required: false, unique: false, start: false };
        // Used to delete from templates
        $scope.deleteTemplateUuid = "";
        // Data that's only used for the view
        $scope.ui = {};
        $scope.ui.databaseInitError = { checked: false, flag: false };
        $scope.ui.submitError = {};
        $scope.ui.submitSucceeded = {};
        $scope.ui.parentMenu = [];

        // This checks if the database is initialized and shows an db-init screen if not
        $scope.checkDatabaseInit = function() {
            // Did we check already? Is the eventbus ready?
            if ($scope.ui.databaseInitError.checked == false) {
                // Query the database for the root-node
                vertxEventBusService.send('com.raaftech.structure.repository.typenodes',
                    {'msgh':'findOne', 'args':{'node':'b1bc963a-19cc-4518-aecb-a35d028c49e3'}})
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
        $scope.databaseInit = function() {
                // Send the db-init message over the eventbus
                vertxEventBusService.send('com.raaftech.structure.manager.schemamanager',
                    {'msgh':'update', 'args':{}}, 20000)
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

        // Clear the error flag so the db-init screen disappears from the view
        $scope.ui.databaseInitErrorClear = function() {
            $timeout(function() {
                $scope.ui.databaseInitError.flagged = false;
            }, 0);
        };


        // Create a new TemplateNode
        $scope.createNewTemplate = function() {
                // Send our newTemplate object over the eventbus
                vertxEventBusService.send('com.raaftech.structure.repository.templatenodes',
                    {'msgh':'putNode', 'args':$scope.newTemplate})
                    .then(function(reply) {
                        // If everything went alright clear the form and show an informational message
                        if (reply.result.error == null) {
                            $timeout(function() {
                                $scope.newTemplate = {};
                                $scope.newTemplateForm.$setPristine(true);
                                $scope.ui.submitError.flag = false;
                                $scope.ui.submitSucceeded.flag = true;
                                $scope.ui.submitSucceeded.message = 'New node has been created with UUID: ' + reply.result.data;
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
        };

        // Delete a TemplateNode, removing it form it's parent (if it has one)
        // and removing all its children.
        $scope.deleteTemplate = function() {
                vertxEventBusService.send('com.raaftech.structure.node.template.'
                    + $scope.deleteTemplateUuid,
                    {'msgh':'removeFromTemplate', 'args':{}})
                    .then(function(reply) {
                        console.log(reply.result);
                    }).catch(function() {
                        console.warn("troubles");
                    });
        }

        // Build object for the dropdown for parent selection
        $scope.getTemplates = function() {
                vertxEventBusService.send('com.raaftech.structure.repository.templatenodes',
                    {'msgh':'findAllTemplates', 'args':{}})
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
                        for (i in reply.result.data) {
                            node = reply.result.data[i];
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
        };

        // Find a TypeNode either by name or by UUID
        $scope.findTypeNode = function() {
                vertxEventBusService.send('com.raaftech.structure.repository.typenodes',
                    {'msgh':'findOne', 'args':{'node': $scope.newTemplate.isa}})
                    .then(function(reply) {
                        console.log(reply);
                        if (reply.result.error == null) {
                            $timeout(function() {
                                $scope.newTemplate.isa = reply.result.data.uuid;
                            });
                        }
                    }).catch(function() {
                        console.warn(reply);
                    });
        };

        // When we click the required checkbox also check required and unique
        $scope.clickStart = function() {
            $scope.newTemplate.required = true;
            $scope.newTemplate.unique = true;
        }
    })

    .controller("containerCtrl", function($scope) {

        $scope.tab = 1;

        $scope.isSelected = function(checkTab) {
            return $scope.tab === checkTab;
        }

        $scope.changeTab = function(setTab) {
            $scope.tab = setTab;
        }
    })

    .controller("instanceCtrl", function($scope, $timeout, schemaForm, vertxEventBus, vertxEventBusService) {
        // Used to generate the forms
        $scope.schemas = [];
        $scope.model = [];

        // Object containing everything that's used for the UI
        $scope.ui = {};
        // Object containing all our templates, eagerly fetched.
        $scope.templates = {};

        // Get all templates from the database.
        $scope.getTemplates = function() {
                vertxEventBusService.send('com.raaftech.structure.repository.templatenodes',
                    {'msgh':'findAllTemplates', 'args':{}})
                    .then(function(reply) {
                        $scope.ui.templates = reply.result.data;
                    });
        };

        // Create a form using the selected template
        $scope.createForm = function(index) {
            function recurse(template) {
                $scope.schemas.push({ name: template.name, uuid: template.uuid, form: template.form, schema: JSON.parse(template.schema)});
                console.log(template.form);
                $scope.model.push({templateNodeUuid: template.uuid, data: {}});
                if (template.children != null) {
                    var childrenLength = template.children.length;
                    if (childrenLength > 0) {
                        for (var i = 0; i < childrenLength; i++) {
                            recurse(template.children[i]);
                        }
                    }
                }
            }

            $scope.schemas = [];
            $scope.model = [];
            $scope.selectedTemplate = $scope.ui.templates[index].uuid;
            recurse($scope.ui.templates[index]);
        }

        // Instantiate the template
        $scope.instantiateTemplate = function() {
                vertxEventBusService.send('com.raaftech.structure.node.template.'
                    + $scope.selectedTemplate,
                    {'msgh':'instantiate', 'args':{'instancedata': $scope.model}})
                    .then(function(reply) {
                        if (reply.result.error == null) {
                            $scope.ui.instantiateSucceeded = {};
                            $scope.ui.instantiateSucceeded.flag = true;
                            $scope.ui.instantiateSucceeded.message = "Created instances starting at uuid " + reply.result.data;
                        } else if (reply.result.error != null) {
                            $scope.ui.instantiateError = {};
                            $scope.ui.instantiateError.flag = true;
                            $scope.ui.instantiateError.message = reply.result.error;
                        }
                    });
        };
    })

    .controller("handleCtrl", function($scope, $timeout, vertxEventBus, vertxEventBusService) {
        $scope.findInstanceNodeUuid = "";
        $scope.taskId = "";
        $scope.foundInstanceNode = {};
        $scope.lastResult = {};

        // Find an InstanceNode by UUID or name
        $scope.findInstanceNode = function() {
                vertxEventBusService.send('com.raaftech.structure.repository.instancenodes',
                    {'msgh':'findOne', 'args':{'node': $scope.findInstanceNodeUuid}})
                    .then(function(reply) {
                        if (reply.result.error == null) {
                            $scope.foundInstanceNode = reply.result.data;
                            console.log(reply.result.data);
                        } else {
                            $scope.foundInstanceNode = {};
                            console.warn(reply.result);
                        }
                    }).catch(function() {
                        console.warn("timeout");
                    });
        }

        // Call a plugin
        $scope.sendMessage = function(address) {
                vertxEventBusService.send("com.raaftech.structure.manager.pluginmanager",
                    {"msgh":"callplugin", "args":{'node': $scope.foundInstanceNode.uuid, 'address': address}})
                    .then(function(reply) {
                        if (reply.result.error == null) {
                            $scope.taskId = reply.result.task;
                        } else {
                            console.warn(reply.result.error);
                        }
                    }).catch(function() {
                        console.warn("timeout");
                    });
        }

        // Get results of an InstanceNode
        $scope.getResults = function(task) {
                vertxEventBusService.send('com.raaftech.structure.node.instance.'
                    + $scope.foundInstanceNode.uuid,
                    {'msgh': 'getResults', 'args':{'task': task}})
                    .then(function(reply) {
                        if (reply.result.error == null) {
                            $scope.lastResult = reply.result;
                            console.log(reply.result.data);
                        } else {
                            console.warn(reply.result);
                        }
                    });
        }
    });