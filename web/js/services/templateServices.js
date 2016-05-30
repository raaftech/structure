angular.module("structureApp")
    .factory("templateSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getTemplate: function(scope, uuid) {
                vertxEventBusService.send('com.raaftech.structure.repository.templatenodes',
                    {
                        'msgh': 'findOne',
                        'args': {
                            'node': uuid
                        }
                    })
                    .then(function(reply) {
                        scope.template = reply.result.data;
                        // get the isa values
                        vertxEventBusService.send('com.raaftech.structure.repository.templatenodes',
                            {
                                'msgh': 'findOne',
                                'args': {
                                    'node': scope.template.isa
                                }
                            })
                            .then(function(reply) {
                                scope.isa = reply.result.data;
                            });
                    });
            },

            getAllTemplates: function(scope) {
                vertxEventBusService.send('com.raaftech.structure.repository.templatenodes',
                    {
                        'msgh': 'findAllTemplates',
                        'args': {}
                    })
                    .then(function(reply) {
                        scope.templates = reply.result.data;
                    });
            },

            getTemplates: function(scope, uuid) {
                vertxEventBusService.send('com.raaftech.structure.node.template.' + uuid,
                    {
                        'msgh': 'getSubTemplates',
                        'args': {}
                    })
                    .then(function(reply) {
                        scope.templates = reply.result.data;
                    });
            },

            getSubTemplates: function(scope, uuid) {
                vertxEventBusService.send('com.raaftech.structure.node.template.' + uuid,
                    {
                        'msgh': 'getSubTemplates',
                        'args': {}
                    })
                    .then(function (reply) {
                        scope.subTemplates[uuid] = reply.result.data;
                    });
            }
        }
    });