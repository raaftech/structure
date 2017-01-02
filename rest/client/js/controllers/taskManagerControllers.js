angular.module("structureApp")
    .controller("taskManagerCtrl", function($scope, taskManagerSrvs) {

        $scope.tasks = [];
        $scope.numberOfTasks = '...';
        $scope.numberOfJobs = '...';

        $scope.testManager = function() {
            taskManagerSrvs.addTestData();
            taskManagerSrvs.queueStatus($scope);
            taskManagerSrvs.liveStatus($scope);
        };

        $scope.updateState = function(data) {
            // Check if the UUID of the task is in the scope
            if(data.data.taskId in $scope.tasks) {
                // If the status is about a job
                if(data.data.jobId >= 0) {
                    // Update the status of the job
                    $scope.tasks[data.data.taskId].jobs[data.data.jobId].status = data.event;
                    if(data.event == 'jobFinished') {
                        $scope.numberOfJobs--;
                        $scope.tasks[data.data.taskId].doneJobs++;
                    }
                }
                // The status is about the task when it's not a job
                else{
                    // Update status of the task
                    $scope.tasks[data.data.taskId].status = data.event;
                    if(data.event == 'taskFinished') {
                        $scope.numberOfTasks--;
                    }
                }
            }
        };

        $scope.cancelTask = function(taskId) {
            taskManagerSrvs.cancelTask(taskId);
        };

        $scope.cancelJob = function(taskId, jobId) {
            taskManagerSrvs.cancelJob(taskId, jobId);
        };

        $scope.pauseTask = function(taskId) {
            taskManagerSrvs.pauseTask(taskId);
        };

        $scope.countTasks = function() {
            var jobs = 0;
            $scope.numberOfTasks = Object.keys($scope.tasks).length;
            $.each($scope.tasks, function(key,task) {
                jobs += task.jobs.length;
                $scope.tasks[key].maxJobs = task.jobs.length;
                $scope.tasks[key].doneJobs = 0;
            });
            $scope.numberOfJobs = jobs;
        };
    });
