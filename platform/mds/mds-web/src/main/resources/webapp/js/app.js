(function () {
    'use strict';

    var mds = angular.module('mds', [ 'motech-dashboard', 'mds.services', 'webSecurity.services',
        'mds.controllers', 'mds.directives', 'mds.utils', 'ui.directives']);

    $.ajax({
        url:      '../mds/available/mdsTabs',
        success:  function(data) {
            mds.constant('AVAILABLE_TABS', data);
        },
        async:    false
    });

    mds.run(function ($rootScope, AVAILABLE_TABS) {
        $rootScope.AVAILABLE_TABS = AVAILABLE_TABS;
    });

    mds.config(function ($routeProvider, AVAILABLE_TABS) {
        $routeProvider.when(
            '/mds/dataBrowser/:entityId',
            {
                templateUrl: '../mds/resources/partials/dataBrowser.html',
                controller: 'MdsDataBrowserCtrl'
            }
        );
        angular.forEach(AVAILABLE_TABS, function (tab) {
            $routeProvider.when(
                '/mds/{0}'.format(tab),
                {
                    templateUrl: '../mds/resources/partials/{0}.html'.format(tab),
                    controller: 'Mds{0}Ctrl'.format(tab.capitalize())
                }
            );
        });
    });
}());
