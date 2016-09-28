angular.module("structureApp")
    .factory("instanceSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getNInstances: function(scope, n) {
                vertxEventBusService.send('com.raaftech.structure.repository.instances',
                    {
                        'msgh': 'nLastCreated',
                        'args': {
                            'amount': n
                        }
                    })
                    .then(function(reply) {
                        scope.instances = reply.result.data;
                    });
            },

            getInstance: function(uuid) {
                return vertxEventBusService.send('com.raaftech.structure.repository.instances', {
                    'msgh': 'findOne',
                    'args': {
                        'node': uuid
                    }
                })
                .then(function(response) {
                    return response.result.data;
                });
            }
        }
    });