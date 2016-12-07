angular.module("structureApp")
    .factory("instrumentSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getInstruments: function(uuid) {
                return vertxEventBusService.send("com.raaftech.structure.node.component." + uuid,
                    {
                        "msgh": "getInstruments",
                        "args": {}
                    })
                    .then(function(response) {
                        return response.result;
                    });
            },

            runInstrument: function(data) {
                var args = {
                    "instrument": data.uuidInstr
                };
                if(data.instrumentname == 'IpAddressDiscover') {
                    args.timeAllocated = 60000;
                }
                return vertxEventBusService.send("com.raaftech.structure.node.component." + data.uuid, {
                        "msgh": "runInstrument",
                        "args": args
                    })
                    .then(function(response) {
                        return response.result;
                    });
            }
        }
    });