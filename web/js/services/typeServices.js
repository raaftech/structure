angular.module("structureApp")
    .factory("typeSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getType: function(scope, uuid) {
                vertxEventBusService.send('com.raaftech.structure.repository.typenodes',
                    {
                        'msgh': 'findOne',
                        'args': {
                            'node': uuid
                        }
                    })
                    .then(function(reply) {
                        scope.type = reply.result.data;
                        // get the isa values
                        vertxEventBusService.send('com.raaftech.structure.repository.typenodes',
                            {
                                'msgh': 'findOne',
                                'args': {
                                    'node': scope.type.isa
                                }
                            })
                            .then(function(reply) {
                                scope.isa = reply.result.data;
                            });
                    });
            },

            getTypes: function(scope, uuid) {
                vertxEventBusService.send('com.raaftech.structure.node.type.' + uuid,
                    {
                        'msgh': 'getSubtypes',
                        'args': {}
                    })
                    .then(function(reply) {
                        scope.types = reply.result.data;
                    });
            },

            getSubtypes: function(scope, uuid) {
                vertxEventBusService.send('com.raaftech.structure.node.type.' + uuid,
                    {
                        'msgh': 'getSubtypes',
                        'args': {}
                    })
                    .then(function (reply) {
                        scope.subtypes[uuid] = reply.result.data;
                    });
            }
        }
    });