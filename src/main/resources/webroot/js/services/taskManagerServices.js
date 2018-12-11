angular.module("structureApp")
    .factory("taskManagerSrvs", function(vertxEventBus, vertxEventBusService, uuidSrvs) {

        return {

            addTestData: function() {
                var i = 0;
                while(i < 6) {
                    i++;
                    vertxEventBusService.send('tech.raaf.structure.bus.manager.taskmanager',
                        {
                            'msgh': 'queueTask',
                            'args': {
                                'taskId': uuidSrvs.generateUuid(),
                                'sequential': true,
                                'jobs': [{
                                    'metadata': {
                                        'originName': 'apache2',
                                        'originUuid': '57d418dc-4193-4b73-ac5f-bce11f569f4b',
                                        'instrument': 'SystemdStoppable',
                                        'capability': 'Stoppable'
                                    },
                                    'command': 'sleep ' + i
                                },{
                                    'metadata': {
                                        'originName': 'sshd',
                                        'originUuid': 'b251f78f-0e03-4882-81f7-db4068e86d5a',
                                        'instrument': 'SystemdStoppable',
                                        'capability': 'Stoppable'
                                    },
                                    'command': 'sleep ' + (i + 1)
                                },{
                                    'metadata': {
                                        'originName': 'dropbox',
                                        'originUuid': '474585de-0aaf-479d-b1d9-995bf75fc866',
                                        'instrument': 'SystemdStoppable',
                                        'capability': 'Stoppable'
                                    },
                                    'command': 'sleep ' + (i + 2)
                                }]
                            }
                        })
                        .then(function(reply) {});
                }
            },

            queueStatus: function(scope) {
                vertxEventBusService.send('tech.raaf.structure.bus.manager.taskmanager',
                    {
                        'msgh': 'queueStatus',
                        'args': {}
                    })
                    .then(function (reply) {
                        scope.tasks = reply.result.data;
                        scope.countTasks();
                    });
            },

            cancelJob: function(taskId, jobId) {
                vertxEventBusService.send('tech.raaf.structure.bus.manager.taskmanager',
                    {
                        'msgh': 'cancelJob',
                        'args': {
                            'taskId': taskId,
                            'jobId': Number(jobId)
                        }
                    });
            },

            cancelTask: function(taskId) {
                vertxEventBusService.send('tech.raaf.structure.bus.manager.taskmanager',
                    {
                        'msgh': 'cancelTask',
                        'args': {
                            'taskId': taskId
                        }
                    });
            },

            pauseTask: function(taskId) {
                vertxEventBusService.send('tech.raaf.structure.bus.manager.taskmanager',
                    {
                        'msgh': 'toggleTaskPause',
                        'args': {
                            'taskId': taskId
                        }
                    });
            },

            liveStatus: function(scope) {
                vertxEventBusService.on('tech.raaf.structure.bus.status', function(result) {
                    scope.updateState(result);
                });
            }
        }
    });