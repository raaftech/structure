angular.module("structureApp")
    .factory("typeSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getType: function(uuid) {
                return vertxEventBusService.send("com.raaftech.structure.repository.types",
                    {
                        "msgh": "findOne",
                        "args": {
                            "node": uuid
                        }
                    })
                    .then(function(response) {
                        return response.result.data;
                    });
            },

            getChildren: function(uuid) {
                return vertxEventBusService.send("com.raaftech.structure.node.type." + uuid,
                    {
                        "msgh": "getSubtypes",
                        "args": {}
                    })
                    .then(function(response) {
                        return response.result.data;
                    });
            },

            getIsaChildren: function(uuid) {
                return vertxEventBusService.send("com.raaftech.structure.node.type." + uuid,
                    {
                        "msgh": "getSubtypes",
                        "args": {}
                    })
                    .then(function(response) {
                        var titleMap = [];
                        angular.forEach(response.result.data, function(node) {
                            titleMap.push({
                                "value": node.uuid,
                                "name": node.name
                            });
                        });
                        return titleMap;
                    });
            }
        }
    });