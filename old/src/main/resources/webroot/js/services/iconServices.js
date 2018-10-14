angular.module("structureApp")
    .factory("iconSrvs", function(vertxEventBus, vertxEventBusService) {

        return {

            getNamesAndGlyphs: function() {
                return vertxEventBusService.send('com.raaftech.structure.bus.repository.types',
                    {
                        'msgh': 'getNamesAndGlyphs',
                        'args': {}
                    })
                    .then(function(response) {
                        var namesAndGlyphs = [];
                        angular.forEach(response.data, function(glyph, name) {
                            namesAndGlyphs = namesAndGlyphs.concat([{
                                'name': name,
                                'glyph': glyph
                            }])
                        })
                        return namesAndGlyphs;
                    });
            }
        }
    });