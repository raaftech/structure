(function() {
	var app = angular.module('universal', ['knalli.angular-vertxbus'])

	app.config(function(vertxEventBusProvider) {
		vertxEventBusProvider
		.enable()
		.useUrlServer('http://localhost:8080');
	});
	
	app.controller('ContainerController', function() {
		this.tab = 1;

		this.isSelected = function(checkTab) {
			return this.tab === checkTab;
		}

		this.changeTab = function(setTab) {
			this.tab = setTab;
		}
	});

	app.controller('TemplateController', ['$scope', '$timeout', 'vertxEventBus', 'vertxEventBusService',
	                                      function($scope, $timeout, vertxEventBus, vertxEventBusService) {
		$scope.newTemplate = { required: false, unique: false, start: false };
		$scope.databaseInitError = { checked: false, flag: false };
		$scope.submitError = {};
		$scope.submitSucceeded = {};

		this.checkDatabaseInit = function() {
			if ($scope.databaseInitError.checked == false && vertxEventBus.readyState()) {
				vertxEventBusService.send('org.openpcf.universal.actor.framehelper',
						{'msgh':'typenode', 'args':{'node':'b1bc963a-19cc-4518-aecb-a35d028c49e3'}})
						.then(function(reply) {
							if (reply.result.error != null) {
								$timeout(function() {
									$scope.databaseInitError.flagged = true;
								}, 0);
							}
						});
				$timeout(function() {
					$scope.databaseInitError.checked = true;
				});
			}
		}

		this.databaseInit = function() {
			if (vertxEventBus.readyState()) {
				vertxEventBusService.send('org.openpcf.universal.actor.schemamanager',
						{'msgh':'update', 'args':{}})
						.then(function(reply) {
							if (reply.result.error == null) {
								$timeout(function() {
									$scope.databaseInitErrorClear();
								}, 0);
							} else {
								alert(reply.result.error);
							}
						});
			}
		}

		$scope.databaseInitErrorClear = function() {
			$timeout(function() {
				$scope.databaseInitError.flagged = false;
			}, 0);
		};


		this.createNewTemplate = function() {
			if (vertxEventBus.readyState()) {
				vertxEventBusService.send('org.openpcf.universal.actor.framehelper',
						{'msgh':'createtemplatenode', 'args':$scope.newTemplate})
						.then(function(reply) {
							if (reply.result.error == null) {
								$timeout(function() {
									$scope.newTemplate = {};
									$scope.newTemplateForm.$setPristine(true);
									$scope.submitError.flag = false;
									$scope.submitSucceeded.flag = true;
									$scope.submitSucceeded.message = 'New node has been created with UUID: ' + reply.result.uuid;
								}, 0);
							} else {
								$timeout(function() {
									$scope.submitSucceeded.flag = false;
									$scope.submitError.flag = true;
									$scope.submitError.message = reply.result.error;
								}, 0);
							}
						}).catch(function() {
							$timeout(function() {
								$scope.submitSucceeded.flag = false;
								$scope.submitError.flag = true;
								$scope.submitError.message = 'Error while communicating with eventbus';
							}, 0);
						});
			}
		};

		this.clickStart = function() {
			$scope.newTemplate.required = true;
			$scope.newTemplate.unique = true;
		}
	}]);
})();