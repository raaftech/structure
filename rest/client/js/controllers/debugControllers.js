angular.module("structureApp")
    .controller("debugCtrl", function($scope, $timeout, createChangeStream, LiveSet) {
        $scope.changeStreamUrl = '/api/Statuses/change-stream?_format=event-stream';
        $scope.src = new EventSource($scope.changeStreamUrl);
        $scope.changes = createChangeStream($scope.src);
        $scope.theSet = new LiveSet([],$scope.changes);

        //$scope.response = $scope.theSet.toLiveArray();

        $scope.changes.on('data', function(update) {
            console.log(JSON.stringify(update));
        });

        $scope.changes.on('end', function() {
            console.log('ChangeStream end from server');
        });

        $scope.changes.on('error', function(error) {
            console.log('ChangeStream error: '+error);
        });
    });
