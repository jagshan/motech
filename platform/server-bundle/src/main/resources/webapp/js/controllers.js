(function () {
    'use strict';

    var serverModule = angular.module('motech-dashboard');

    serverModule.controller('MotechMasterCtrl', function ($scope, $http, i18nService, $cookieStore, $q, BrowserDetect, Menu, $location, $timeout, $route) {

        var handle = function () {
                if (!$scope.$$phase) {
                    $scope.$digest();
                }

                $scope.ready = true;
            },
            checkForRefresh = function () {
                if (window.location.hash !== "" && $scope.user.anonymous !== true) {
                    var start_pos = window.location.hash.indexOf('/') + 1,
                        end_pos = window.location.hash.indexOf('/', start_pos);
                    if (end_pos < 0) {
                        end_pos = window.location.hash.length;
                    }
                    $scope.loadModule(window.location.hash.substring(start_pos, end_pos), "/" + window.location.hash.substring(start_pos, window.location.hash.length));
                } else {
                    window.location.hash = '';
                }
            };

        $scope.BrowserDetect = BrowserDetect;

        $scope.ready = false;
        $scope.i18n = {};
        $scope.languages = [];
        $scope.contextPath = '';
        $scope.userLang = {};
        $scope.pagedItems = [];
        $scope.currentPage = 0;
        $scope.config = {};
        $scope.bodyState = true;
        $scope.outerLayout = {};
        $scope.innerLayout = {};
        $scope.user = {
            anonymous: true
        };

        $scope.moduleToLoad = undefined;
        $scope.activeLink = undefined;
        $scope.documentationUrl = undefined;
        $scope.activeMenu = "servermodules";

        $scope.loginViewData = {};
        $scope.resetViewData = {};
        $scope.startupViewData = {};
        $scope.forgotViewData = {};

        $scope.showDashboardLogo = {
            showDashboard : true,
            changeClass : function () {
                return this.showDashboard ? "fa fa-caret-up" : "fa fa-caret-down";
            },
            changeTitle : function () {
                return this.showDashboard ? "server.minimizeLogo" : "server.expandLogo";
            },
            backgroundUpDown : function () {
                return this.showDashboard ? "body-down" : "body-up";
            },
            changeHeight : function () {
                return this.showDashboard ? "100" : "40";
            }
        };

        $scope.changeName = function (name) {
            return name.replace('.', "");
        };

        $scope.showActiveMenu = {
            hideSection : function (modulesSection) {
                var changedModulesSection = $scope.changeName(modulesSection);
                return $scope.activeMenu === changedModulesSection ? "" : "hidden";
            },
            changeClass : function (modulesSection) {
                var changedModulesSection = $scope.changeName(modulesSection.name);
                if (changedModulesSection === $scope.activeMenu) {
                    if (modulesSection.name === "server.modules" && $scope.activeLink !== undefined && $scope.activeLink.moduleName !== "admin"
                        && $scope.activeLink.moduleName !== "webSecurity" && $scope.activeLink.moduleName !== "rest-docs") {
                        $scope.documentationUrl = $scope.lastModulesActiveMenu;
                    } else {
                        $scope.documentationUrl = modulesSection.moduleDocsUrl;
                    }
                    return "active";
                }
                return "";
            }
        };

        $scope.setDocsUrl = function (url, modulesSection) {
            $scope.documentationUrl = url;
            if (modulesSection) {
                $scope.lastModulesActiveMenu = url;
            }
        };

        $scope.setUserLang = function (lang, refresh) {
            var locale = toLocale(lang), setLangUrl = "userlang";

            if ($scope.isStartupView()) {
                setLangUrl = "lang/session"; // For startup settings we set the session language only, since there's no user yet
            }

            $http({ method: "POST", url: setLangUrl, params: locale })
                .success(function () {
                    $scope.doAJAXHttpRequest('GET', 'lang/locate', function (data) {
                        $scope.i18n = data;
                        $scope.loadI18n($scope.i18n);
                    });

                    if ($scope.isStartupView()) {
                        $scope.startupViewData.startupSettings.language = lang;
                    }

                    moment.lang(lang);
                    motechAlert('server.success.changed.language', 'server.changed.language',function(){
                        if (refresh ) {
                            window.location.reload();
                        }
                    });
                })
                .error(function (response) {
                    handleResponse('server.header.error', 'server.error.setLangError', response);
                });
        };

        $scope.isStartupView = function() {
            return ($scope.startupViewData && $scope.startupViewData.startupSettings);
        };

        $scope.msg = function () {
            return i18nService.getMessage(arguments);
        };

        $scope.getLanguage = function (locale) {
            return {
                key: locale.toString() || "en",
                value: $scope.languages[locale.toString()] || $scope.languages[locale.withoutVariant()] || $scope.languages[locale.language] || "English"
            };
        };

        $scope.minimizeHeader = function () {
            $scope.showDashboardLogo.showDashboard = !$scope.showDashboardLogo.showDashboard;
            $scope.outerLayout.sizePane('north', $scope.showDashboardLogo.changeHeight());
            $cookieStore.put("showDashboardLogo", $scope.showDashboardLogo.showDashboard);
        };

        $scope.storeSelected = function () {
            $cookieStore.put("activeMenu", $scope.activeMenu);
        };

        $scope.selectModules = function (sectionName) {
            var sectionNameChanged = $scope.changeName(sectionName);
            $scope.activeMenu = sectionNameChanged;
            $scope.storeSelected();
        };

        $scope.doAJAXHttpRequest = function (method, url, callback) {
            var defer = $q.defer();

            $http({ method: method, url: url }).
                success(function (data) {
                    callback(data);
                    defer.resolve(data);
                });

            return defer.promise;
        };

        $scope.loadModule = function (moduleName, url) {
            var refresh, resultScope, reloadModule;
            $scope.activeLink = {moduleName: moduleName, url: url};
            if (moduleName) {
                blockUI();

                $http.get('../server/module/critical/' + moduleName).success(function (data, status) {
                    if (data !== undefined && data !== '' && status !== 408) {
                        jAlert(data, $scope.msg('error'));
                    }
                });

                if ($scope.moduleToLoad === moduleName || url === '/login') {
                    $location.path(url);
                    unblockUI();
                    innerLayout({}, {
                        show: false
                    });
                } else {
                    refresh = ($scope.moduleToLoad === undefined) ? true : false;
                    $scope.moduleToLoad = moduleName;

                    if (url) {
                        reloadModule = true;
                        window.location.hash = "";
                        $scope.$on('loadOnDemand.loadContent', function () {
                            if (reloadModule) {
                                $location.path(url);
                                unblockUI();
                                reloadModule = false;
                                innerLayout({}, {
                                    show: false
                                });
                                if (refresh) {
                                    $route.reload();
                                }
                            }
                        });
                    } else {
                        unblockUI();
                    }
                }
            }
        };

        $scope.loadI18n = function (data) {
            i18nService.init(data);
            handle();
            checkForRefresh();
        };

        $scope.resetItemsPagination = function () {
            $scope.pagedItems = [];
            $scope.currentPage = 0;
        };

        $scope.groupToPages = function (filteredItems, itemsPerPage) {
            var i;
            $scope.pagedItems = [];

            for (i = 0; i < filteredItems.length; i += 1) {
                if (i % itemsPerPage === 0) {
                    $scope.pagedItems[Math.floor(i / itemsPerPage)] = [ filteredItems[i] ];
                } else {
                    $scope.pagedItems[Math.floor(i / itemsPerPage)].push(filteredItems[i]);
                }
            }
        };

        $scope.range = function (start, end) {
            var ret = [], i;
            if (!end) {
                end = start;
                start = 0;
            }
            for (i = start; i < end; i += 1) {
                ret.push(i);
            }
            return ret;
        };

        $scope.setCurrentPage = function (currentPage) {
            $scope.currentPage = currentPage;
        };

        $scope.firstPage = function () {
            $scope.currentPage = 0;
        };

        $scope.lastPage = function (lastPage) {
            $scope.currentPage = lastPage;
        };

        $scope.prevPage = function () {
            if ($scope.currentPage > 0) {
                $scope.currentPage -= 1;
            }
        };

        $scope.nextPage = function () {
            if ($scope.currentPage < $scope.pagedItems.length - 1) {
                $scope.currentPage += 1;
            }
        };

        $scope.setPage = function () {
            $scope.currentPage = this.number;
        };

        $scope.hidePages = function (number) {
            return ($scope.currentPage + 4 < number && number > 8) || ($scope.currentPage - 4 > number && number + 9 < $scope.pagedItems.length);
        };

        $scope.printDate = function (milis) {
            var date = "";
            if (milis) {
                date = new Date(milis);
            }
            return date;
        };

        $scope.getCurrentModuleName = function () {
            var queryKey = parseUri(window.location.href).queryKey;
            return (queryKey && queryKey.moduleName) || '';
        };

        $scope.getCurrentAnchor = function () {
            return parseUri(window.location.href).anchor;
        };

        $scope.active = function(url) {
            if (window.location.href.indexOf(url) !== -1) {
                return "active";
            }
        };

        $scope.safeApply = function (fun) {
            var phase = this.$root.$$phase;

            if (phase === '$apply' || phase === '$digest') {
                if(fun && (typeof(fun) === 'function')) {
                    fun();
                }
            } else {
                this.$apply(fun);
            }
        };

        $scope.BrowserDetect.init();

        if ($cookieStore.get("showDashboardLogo") !== undefined) {
            $scope.showDashboardLogo.showDashboard=$cookieStore.get("showDashboardLogo");
        }

        if ($cookieStore.get("activeMenu") !== undefined) {
            $scope.activeMenu = $cookieStore.get("activeMenu");
        }

        $q.all([
            $scope.doAJAXHttpRequest('GET', 'lang/locate', function (data) {
                $scope.i18n = data;
            }),
            $scope.doAJAXHttpRequest('GET', 'lang/list', function (data) {
                $scope.languages = data;
            }),
            $scope.doAJAXHttpRequest('GET', 'lang', function (data) {
                $scope.user.lang = data;
            })
        ]).then(function () {
            $scope.userLang = $scope.getLanguage(toLocale($scope.user.lang));
            moment.lang($scope.user.lang);
            $scope.loadI18n($scope.i18n);
        });

        $scope.$on('lang.refresh', function () {
            $scope.ready = false;

            $q.all([
                $scope.doAJAXHttpRequest('GET', 'lang/locate', function (data) {
                    $scope.i18n = data;
                }), $scope.doAJAXHttpRequest('GET', 'lang/list', function (data) {
                    $scope.languages = data;
                })
            ]).then(function () {
                $scope.userLang = $scope.getLanguage(toLocale($scope.user.lang));
                moment.lang($scope.user.lang);
                $scope.loadI18n($scope.i18n);
            });
        });

        $scope.setSuggestedValue = function(ngtarget, ngkey, value) {
            ngtarget[ngkey] = value;
        };

        $scope.getUrlVar = function (key) {
            var result = new RegExp(key + "=([^&]*)", "i").exec(window.location.search);
            return result || "";
        };

        $scope.getLoginViewData = function() {
            $scope.doAJAXHttpRequest('GET', 'loginviewdata', function (data) {
                var parameter = $scope.getUrlVar("error");
                $scope.loginViewData = data;
                $scope.loginContextPath = $scope.loginViewData.contextPath + 'j_spring_security_check';
                $scope.loginContextPathOpenId = $scope.loginViewData.contextPath + 'j_spring_openid_security_check';

                if (parameter !== '') {
                    $scope.loginViewData.error = parameter;
                }
            });
        };

        $scope.getResetViewData = function() {
            var parametr = window.location.search;

            $scope.doAJAXHttpRequest('GET', '../server/forgotresetviewdata' + parametr, function (data) {
                $scope.resetViewData = data;
            });
        };

        $scope.submitResetPasswordForm = function() {
            blockUI();

            $http({
                method: 'POST',
                url: '../server/forgotreset',
                data: $scope.resetViewData.resetForm
            }).success(function(data) {
                unblockUI();

                if (data.errors === undefined || data.errors.length === 0) {
                    data.errors = null;
                }

                $scope.resetViewData = data;
            })
            .error(function(data) {
                unblockUI();
                motechAlert('server.reset.error');
                $scope.resetViewData.errors = ['server.reset.error'];
            });
        };

        $scope.getStartupViewData = function() {
            $scope.doAJAXHttpRequest('GET', '../server/startupviewdata', function (data) {
                $scope.startupViewData = data;
            });
        };

        $scope.submitStartupConfig = function() {
             $scope.startupViewData.startupSettings.loginMode = $scope.securityMode;
             $http({
                method: "POST",
                url: "../server/startup/",
                data: $scope.startupViewData.startupSettings
             })
             .success(function(data) {
                if (data.length === 0) {
                    window.location.assign("../server/");
                }
                $scope.errors = data;
             });
        };

        $scope.getForgotViewData = function() {
            $scope.doAJAXHttpRequest('GET', 'forgotviewdata', function (data) {
                $scope.forgotViewData = data;
            });
        };

        $scope.sendReset = function () {
             if ($scope.forgotViewData.email !== "") {
                 $http({ method: "POST", url: "../server/forgot", data: $scope.forgotViewData.email})
                     .success(function (response) {
                        $scope.error=response;
                        $scope.forgotViewData.emailGetter=false;
                        $scope.forgotViewData.processed=true;
                     })
                     .error(function (response) {
                        $scope.error = 'security.tokenSendError';
                     });
             }
        };
    });

    serverModule.controller('MotechHomeCtrl', function ($scope, $cookieStore, $q, Menu, $rootScope, $http) {
        $scope.securityMode = false;

        $scope.moduleMenu = {};

        $scope.openInNewTab = function (url) {
            var win = window.open(url, '_blank');
            win.focus();
        };

        $scope.hasMenu = function(menuName){
            var hasMenuWithGivenName, menuSections ;
            hasMenuWithGivenName = false;
            menuSections  = $scope.moduleMenu.sections;
            angular.forEach(menuSections,function(section){
                if(section.name === menuName){
                    hasMenuWithGivenName = true;
                }
            });
            return hasMenuWithGivenName;
        };


        $scope.isActiveLink = function(link) {
            return $scope.activeLink && $scope.activeLink.moduleName === link.moduleName && $scope.activeLink.url === link.url;
        };

        if ($cookieStore.get("showDashboardLogo") !== undefined) {
            $scope.showDashboardLogo.showDashboard = $cookieStore.get("showDashboardLogo");
        }

        $q.all([
            $scope.moduleMenu = Menu.get(function(data) {
                $scope.moduleMenu = data;
            }, angularHandler('error', 'server.error.cantLoadMenu')),

            $scope.doAJAXHttpRequest('POST', 'getUser', function (data) {
                var scope = angular.element("body").scope();

                scope.user.userName = data.userName;
                scope.user.securityLaunch = data.securityLaunch;
                scope.user.anonymous = false;

                if (!$scope.$$phase) {
                    $scope.$apply(scope.user);
                }

                // set in the rootScope for other modules
                $rootScope.username = data.userName;
            })
        ]).then(function () {
            if ($scope.user.lang) {
                $scope.userLang = $scope.getLanguage(toLocale($scope.user.lang));
                moment.lang($scope.user.lang);
            }
        });

        $scope.$on('module.list.refresh', function () {
            Menu.get(function(data) {
                $scope.moduleMenu = data;
            }, angularHandler('error', 'server.error.cantLoadMenu'));
        });

        jgridDefaultSettings();
    });
}());
