angular.module("structureApp")
    .factory("taskManagerSrvs", function(vertxEventBus, vertxEventBusService, uuidSrvs) {

        return {

            addTestData: function() {
                var i = 0;
                while(i < 6) {
                    i++;
                    vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
                        {
                            'msgh': 'queueTask',
                            'args': {
                                'taskId': uuidSrvs.generateUuid(),
                                'jobs': [{
                                    'metadata': {
                                        'patient': 'apache2',
                                        'instrument': 'SystemdStoppable',
                                        'capability': 'Stoppable'
                                    },
                                    'command': 'sleep ' + i
                                },{
                                    'metadata': {
                                        'patient': 'sshd',
                                        'instrument': 'SystemdStoppable',
                                        'capability': 'Stoppable'
                                    },
                                    'command': 'sleep ' + (i + 1)
                                },{
                                    'metadata': {
                                        'patient': 'dropbox',
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
                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
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
                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
                    {
                        'msgh': 'cancelJob',
                        'args': {
                            'taskId': taskId,
                            'jobId': Number(jobId)
                        }
                    });
            },

            cancelTask: function(taskId) {
                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
                    {
                        'msgh': 'cancelTask',
                        'args': {
                            'taskId': taskId
                        }
                    });
            },

            pauseTask: function(taskId) {
                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
                    {
                        'msgh': 'pauseTask',
                        'args': {
                            'taskId': taskId
                        }
                    });
            },

            liveStatus: function(scope) {
                vertxEventBusService.on('com.raaftech.structure.status', function(result) {
                    scope.updateState(result);
                });
            }
        }
    });