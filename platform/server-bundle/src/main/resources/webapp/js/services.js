(function () {
    'use strict';

    var uiServices = angular.module('uiServices', ['ngResource']);

    uiServices.factory('Menu', function($resource) {
        return $resource('module/menu');
    });

    uiServices.factory('Critical', function($resource) {
        return $resource('module/critical/:moduleName');
    });

}());
