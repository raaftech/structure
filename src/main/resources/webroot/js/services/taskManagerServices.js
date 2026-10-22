angular.module("structureApp")
    .factory("taskManagerSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

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