angular.module("structureApp")
    .factory("instrumentSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getInstruments: function(uuid) {
                return vertxEventBusService.send('com.raaftech.structure.node.instance.' + uuid,
                    {
                        'msgh': 'getInstruments',
                        'args': {}
                    })
                    .then(function(response) {
                        return response.result;
                    });
            },

            runInstrument: function(data) {
                return vertxEventBusService.send('com.raaftech.structure.node.instance.' + data.uuid,
                    {
                        'msgh': 'runInstrument',
                        'args': {
                            'instrument': data.uuidInstr
                        }
                    })
                    .then(function(response) {
                        return response.result;
                    });
            }
        }
    });