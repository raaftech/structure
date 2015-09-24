(function() {
	
	'use strict';

	function showMenuOnHover() {
		return {
    		restrict: 'A',
    		link: function(scope, element, attr) {

    			element.bind('mouseenter', function() {
					var child = element.children(":first");
					var tools = $('<ul>', {class: 'tools'});
					$('<a>', {text: 'Start',  'href': '#'}).appendTo($('<li>').appendTo(tools));
					$('<a>', {text: 'Stop',   'href': '#'}).appendTo($('<li>').appendTo(tools));
					$('<a>', {text: 'Edit',   'href': '#'}).appendTo($('<li>').appendTo(tools));
					$('<a>', {text: 'Remove', 'href': '#'}).appendTo($('<li>').appendTo(tools));
					tools.appendTo(child);
    			});

    			element.bind('mouseleave', function() {
    				$('ul.tools').remove();
    			});
    		}
    	};
	}
	
	function testController($scope) {
		$scope.version = '1.0';
	}
	
	testController.$inject = ['$scope'];

	angular
		.module('app')
		.controller('testController', testController)
		.directive('showMenuOnHover', showMenuOnHover);

})();