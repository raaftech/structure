angular.module("structureApp")
    .factory("searchSrvs", function() {

        return {
            data: {
                query: '',
                error: '',
                results: ''
            },
            start: function(query) {
                return Promise.resolve({query: query, error: '', results: ''});
            }
        }
    });
