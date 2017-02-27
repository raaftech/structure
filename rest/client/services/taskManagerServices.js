angular.module("structureApp")
    .factory("taskManagerSrvs", function(Task, uuidSrvs) {

        return {

            addTestData: function() {
                var promises = [];
                var jobs = function(i) {
                          return [{
                               'metadata': {
                                   'originName': 'apache2',
                                   'originUuid': '57d418dc-4193-4b73-ac5f-bce11f569f4b',
                                   'instrument': 'SystemdStoppable',
                                   'capability': 'Stoppable'
                               },
                               'command': 'sleep ' + i
                           },
                           {
                               'metadata': {
                                   'originName': 'sshd',
                                   'originUuid': 'b251f78f-0e03-4882-81f7-db4068e86d5a',
                                   'instrument': 'SystemdStoppable',
                                   'capability': 'Stoppable'
                               },
                               'command': 'sleep ' + (i + 1)
                           },
                           {
                               'metadata': {
                                   'originName': 'dropbox',
                                   'originUuid': '474585de-0aaf-479d-b1d9-995bf75fc866',
                                   'instrument': 'SystemdStoppable',
                                   'capability': 'Stoppable'
                               },
                               'command': 'sleep ' + (i + 2)
                           }]};
                for(var i = 0; i < 6; i++) {
                    var p = new Promise((resolve, reject) => {
                      Task.queueTask({isSequential: true, jobs: jobs(0)}, function(result) {
                        resolve(result.result);
                      });
                    });
                    promises.push(p);
                }
                Promise.all(promises).then((reply) => console.log('done'));
            },

            queueStatus: function() {
                return new Promise((resolve, reject) => {
                  Task.queueStatus(function(status) {
                      resolve(status.result);
                  })
                });

//TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
//                    {
//                        'msgh': 'queueStatus',
//                        'args': {}
//                    })
//                    .then(function (reply) {
//                        scope.tasks = reply.result.data;
//                        scope.countTasks();
//                    });
            },

            cancelJob: function(taskId, jobId) {
                return new Promise((resolve, reject) => {
                    Task.cancelJob({taskId: taskId, jobId: jobId}, function(result) {
                        resolve(result.result);
                    });
                });

//TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
//                    {
//                        'msgh': 'cancelJob',
//                        'args': {
//                            'taskId': taskId,
//                            'jobId': Number(jobId)
//                        }
//                    });
            },

            cancelTask: function(taskId) {
                return new Promise((resolve, reject) => {
                    Task.cancelTask({taskId: taskId}, function(result) {
                        resolve(result.result);
                    });
                });

//TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
//                    {
//                        'msgh': 'cancelTask',
//                        'args': {
//                            'taskId': taskId
//                        }
//                    });
            },

            pauseTask: function(taskId) {
                return new Promise((resolve, reject) => {
                    Task.pauseTask({taskId: taskId}, function(result) {
                        resolve(result.result);
                    });
                });

//TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.manager.taskmanager',
//                    {
//                        'msgh': 'toggleTaskPause',
//                        'args': {
//                            'taskId': taskId
//                        }
//                    });
            },

            liveStatus: function(scope) {
            //FIXME create replacement code if required
//                vertxEventBusService.on('com.raaftech.structure.status', function(result) {
//                    scope.updateState(result);
//                });
            }
        }
    });
