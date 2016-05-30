angular.module("structureApp")
    .factory("instanceSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getNInstances: function(scope, n) {
                vertxEventBusService.send('com.raaftech.structure.repository.instancenodes',
                    {
                        'msgh': 'nLastCreated',
                        'args': {
                            'amount': n
                        }
                    })
                    .then(function(reply) {
                        scope.instances = reply.result.data;
                    });
            }
        }
    });