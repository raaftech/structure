angular.module("structureApp")
    .factory("instrumentSrvs", function (Entity) {

        return {

            getInstruments: function(uuid) {
                return new Promise((resolve, reject) => {
                    Entity.getInstruments({uuid: uuid}, function (entity) {
                        resolve(entity.result);
                    });
                });

                //TODO remove this code when verified that above code replaces it correctly
                /*
                return vertxEventBusService.send("com.raaftech.structure.node.component." + uuid,
                    {
                        "msgh": "getInstruments",
                        "args": {}
                    })
                    .then(function(response) {
                        return response.result;
                    });
                 */
            },

            //TODO validate that the used parameters are okey. (e.g what are uuidInstr, instrumentname, uuid in the old call)
            runInstrument: function(data) {
                return new Promise((resolve, reject) => {
                    Entity.runInstrument({
                        instrumentName: data.instrumentname,
                        uuid: data.uuid
                    }, function (entity) {

                    });
                });

                //TODO remove this code when verified that above code replaces it correctly
                /*
                var args = {
                    "instrument": data.uuidInstr
                };
                if(data.instrumentname == 'IpAddressDiscover') {
                    args.timeAllocated = 600000;
                }

                return vertxEventBusService.send("com.raaftech.structure.node.component." + data.uuid, {
                        "msgh": "runInstrument",
                        "args": args
                    })
                    .then(function(response) {
                        return response.result;
                    });
                 */
            }
        }
    });