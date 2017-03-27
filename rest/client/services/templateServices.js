angular.module("structureApp")
    .factory("templateSrvs", function(Entity) {

        return {

            getTemplate: function(uuid) {
                return new Promise((resolve, reject) => {
                  Entity.getTemplate({node: uuid}, function(entity) {
                    //console.log(JSON.stringify(entity.result));
                    resolve(entity.result);
                  });
                });

//TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.repository.templates',
//                    {
//                        'msgh': 'findOne',
//                        'args': {
//                            'node': uuid
//                        }
//                    })
//                    .then(function(reply) {
//                        scope.template = reply.result.data;
//                        // get the isa values
//                        vertxEventBusService.send('com.raaftech.structure.repository.templates',
//                            {
//                                'msgh': 'findOne',
//                                'args': {
//                                    'node': scope.template.isa
//                                }
//                            })
//                            .then(function(reply) {
//                                scope.isa = reply.result.data;
//                            });
//                    });
            },

            getAllTemplates: function() {
                return new Promise((resolve, reject) => {
                  Entity.getTemplates(function(templates) {
                    resolve(templates.result);
                  });
                });

//TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.repository.templates',
//                    {
//                        'msgh': 'findAllTemplates',
//                        'args': {}
//                    })
//                    .then(function(reply) {
//                        scope.templates = reply.result.data;
//                    });
            },

            getTemplates: function(uuid) {
                console.log('getTemplates called');
                return new Promise((resolve, reject) => {
                  Entity.getSubTemplates({uuid: uuid}, function(templates) {
                    resolve(templates.result);
                  });
                });

//TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.node.template.' + uuid,
//                    {
//                        'msgh': 'getSubTemplates',
//                        'args': {}
//                    })
//                    .then(function(reply) {
//                        scope.templates = reply.result.data;
//                    });
            },

            getSubTemplates: function(uuid) {
              console.log('getSubTemplates called');
              return getTemplates(uuid);

              //TODO remove this code when verified that above code replaces it correctly
//                vertxEventBusService.send('com.raaftech.structure.node.template.' + uuid,
//                    {
//                        'msgh': 'getSubTemplates',
//                        'args': {}
//                    })
//                    .then(function (reply) {
//                        scope.subTemplates[uuid] = reply.result.data;
//                    });
            }
        }
    });
