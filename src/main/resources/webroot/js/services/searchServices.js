angular.module("structureApp")
    .factory("searchSrvs", function(vertxEventBus, vertxEventBusService) {

        return {
            data: {
                query: '',
                error: '',
                results: ''
            },
            start: function(query) {
                if(query != '') {
                    this.data.query = query;
                    var data = this.data;
                    vertxEventBusService.send('com.raaftech.structure.repository.instances',
                        {
                            'msgh': 'findByName',
                            'args': {
                                'name': this.data.query
                            }
                        })
                        .then(function(reply) {
                            if(reply.result.error) {
                                data.error = reply.result.error;
                                data.results = '';
                            }
                            else{
                                data.error = '';
                                data.results = reply.result.data;
                            }
                        });
                }
            }
        }
    });