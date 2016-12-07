angular.module("structureApp")
    .factory("instanceSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getNInstances: function(n) {
                return vertxEventBusService.send("com.raaftech.structure.repository.instances",
                    {
                        "msgh": "nLastCreated",
                        "args": {
                            "amount": n
                        }
                    })
                    .then(function(response) {
                        return response.result.data;
                    });
            },

            getInstance: function(uuid) {
                return vertxEventBusService.send("com.raaftech.structure.repository.instances", {
                    "msgh": "findOne",
                    "args": {
                        "node": uuid
                    }
                })
                .then(function(response) {
                    return response.result.data;
                });
            },

            putNode: function(args) {
                return vertxEventBusService.send("com.raaftech.structure.repository.instances", {
                        "msgh": "putNode",
                        "args": args
                    })
                    .then(function(response) {
                        return response;
                    });
            },

            liveStatus: function(scope) {
                vertxEventBusService.on("com.raaftech.structure.status", function(result) {
                    scope.logStatus(result);
                });
            }
        }
    });