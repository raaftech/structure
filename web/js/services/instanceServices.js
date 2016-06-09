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
            },

            getInstance: function(uuid) {
                return vertxEventBusService.send('com.raaftech.structure.repository.instancenodes', {
                    'msgh': 'findOne',
                    'args': {
                        'node': uuid
                    }
                })
                .then(function(response) {
                    return response.result.data;
                });
            },

            getRaafMachines: function() {
                return vertxEventBusService.send('com.raaftech.structure.repository.instancenodes',
                    {
                        'msgh': 'getRaafMachines',
                        'args': {}
                    })
                    .then(function(response) {
                        return response.result;
                    });
            },

            getRaafMachine: function(data) {
                return vertxEventBusService.send('com.raaftech.structure.repository.instancenodes',
                    {
                        'msgh': 'getRaafMachine',
                        'args': {
                            'uuid': data.uuid
                        }
                    })
                    .then(function(response) {
                        return response.result.data;
                    });
            }
        }
    });