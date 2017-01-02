angular.module("structureApp").
    controller("templateCtrl", function($scope, templateSrvs) {
        $scope.template = {name: 'test'};
        $scope.templates = [];

        $scope.getAllTemplates = function(uuid) {
            // reset (sub)templates
            //$scope.templates = '';
            $scope.subTemplates = [];
            // get data
            if(uuid) {
                templateSrvs.getTemplate(uuid)
                  .then((template) => {

                    $scope.template = template;
                    return template;
                  })
                  .then((template) => {
                    templateSrvs.getTemplate(template.isa);
                  })
                  .then((isaTemplate) => {
                    $scope.isa = isaTemplate;
                  });


                templateSrvs.getTemplates(uuid)
                  .then((templates) => {
                    $scope.templates = templates;
                  });

//                templateSrvs.getTemplate($scope, uuid);
//                templateSrvs.getTemplates($scope, uuid);
            }
            else{
                templateSrvs.getAllTemplates()
                  .then((templates) => {
                      $scope.templates = templates;
                      console.log($scope.templates);
                      $scope.template = templates[0];
                      console.log($scope.template);
                    });
                //templateSrvs.getAllTemplates($scope);
            }
        };

        $scope.getSubTemplates = function(uuid) {
            if($scope.subTemplates[uuid]) {
                delete $scope.subTemplates[uuid];
            }
            else{
                templateSrvs.getSubTemplates(uuid)
                  .then((templates) => {
                    $scope.subTemplates[uuid] = templates;
                  });

                //templateSrvs.getSubTemplates($scope, uuid);
            }
        }
    });
