System.register(['angular2/core'], function(exports_1, context_1) {
    "use strict";
    var __moduleName = context_1 && context_1.id;
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1;
    var DEFAULT_AVATAR_URL, DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_SNACK_TIMEOUT, SWELLRT_SERVER, SwellRTService;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            }],
        execute: function() {
            DEFAULT_AVATAR_URL = 'images/user.jpeg';
            DEFAULT_USERNAME = '_anonymous_';
            DEFAULT_PASSWORD = '';
            DEFAULT_SNACK_TIMEOUT = 3000;
            SWELLRT_SERVER = 'http://demo.swellrt.org';
            SwellRTService = (function () {
                function SwellRTService() {
                }
                SwellRTService.prototype.ngOnInit = function () {
                    this.object = new Promise(function (resolve, reject) {
                        reject();
                    });
                    this.user = new Promise(function (resolve, reject) {
                        reject();
                    });
                };
                SwellRTService.prototype.bindListeners = function () {
                    console.log("SwellRT listeners bound");
                    SwellRT.on(SwellRT.events.NETWORK_CONNECTED, function () {
                        if (this.lastSnack)
                            this.lastSnack.hide();
                        jQuery.snackbar({ content: "Connected to server", timeout: DEFAULT_SNACK_TIMEOUT });
                        this.state = "CONNECTED";
                        if (this.listener)
                            this.listener(SwellRT.events.NETWORK_CONNECTED);
                    });
                    SwellRT.on(SwellRT.events.NETWORK_DISCONNECTED, function () {
                        this.lastSnack = jQuery.snackbar({ content: "Connection lost trying to reconnect...", timeout: 0 });
                        this.state = "DISCONNECTED";
                        if (this.listener)
                            this.listener(SwellRT.events.NETWORK_DISCONNECTED);
                    });
                    SwellRT.on(SwellRT.events.FATAL_EXCEPTION, function () {
                        if (this.lastSnack)
                            this.lastSnack.hide();
                        this.lastSnack = jQuery.snackbar({ content: "Oops! an fatal error has ocurred. Please <a href='/'>refresh.</a>", timeout: 0, htmlAllowed: true });
                        this.state = "EXCEPTION";
                        if (this.listener)
                            this.listener(SwellRT.events.FATAL_EXCEPTION);
                    });
                };
                SwellRTService.prototype.getState = function () {
                    return this.state;
                };
                SwellRTService.prototype.setListener = function (_listener) {
                    this.listener = _listener;
                };
                SwellRTService.prototype.resume = function (_loginIfError) {
                    var _this = this;
                    var adaptSessionToUser = function (session) { return _this.adaptSessionToUser(session); };
                    var login = function (name, password) { return _this.login(name, password); };
                    this.user = new Promise(function (resolve, reject) {
                        SwellRT.resumeSession(function (session) {
                            var user = adaptSessionToUser(session);
                            if (!user.anonymous)
                                jQuery.snackbar({ content: "Welcome " + user.name, timeout: DEFAULT_SNACK_TIMEOUT });
                            resolve(user);
                        }, function (error) {
                            if (_loginIfError) {
                                login(DEFAULT_USERNAME, DEFAULT_PASSWORD)
                                    .then(function (user) { return resolve(user); })
                                    .catch(function (error) { return reject(error); });
                            }
                            else {
                                reject(error);
                            }
                        });
                    });
                    return this.user;
                };
                SwellRTService.prototype.login = function (_name, _password) {
                    var _this = this;
                    var adaptSessionToUser = function (session) { return _this.adaptSessionToUser(session); };
                    this.user = new Promise(function (resolve, reject) {
                        SwellRT.startSession(SWELLRT_SERVER, _name, _password, function (session) {
                            var user = adaptSessionToUser(session);
                            if (!user.anonymous)
                                jQuery.snackbar({ content: "Welcome " + user.name, timeout: DEFAULT_SNACK_TIMEOUT });
                            resolve(user);
                        }, function (e) {
                            reject(e);
                        });
                    });
                    return this.user;
                };
                SwellRTService.prototype.createUser = function (id, password) {
                    this.user = new Promise(function (resolve, reject) {
                        SwellRT.createUser({ id: id, password: password }, function (res) {
                            if (res.error) {
                                reject(res.error);
                            }
                            else if (res.data) {
                                resolve(res.data);
                            }
                        });
                    });
                    return this.user;
                };
                SwellRTService.prototype.logout = function (_loginAsAnonymous) {
                    this.session = undefined;
                    try {
                        SwellRT.stopSession();
                    }
                    catch (e) {
                        console.log(e);
                    }
                    if (_loginAsAnonymous) {
                        return this.login(DEFAULT_USERNAME, DEFAULT_PASSWORD);
                    }
                    this.user = new Promise(function (resolve, reject) {
                        resolve(null);
                    });
                    return this.user;
                };
                SwellRTService.prototype.getUser = function () {
                    return this.user;
                };
                SwellRTService.prototype.adaptSessionToUser = function (_session) {
                    this.session = _session;
                    var n;
                    // Remove this condition block when
                    // SwellRT returns same data in both resume() and startSession()
                    if (this.session.data)
                        n = this.session.data.id;
                    else
                        n = this.session.address;
                    n = n.slice(0, n.indexOf("@"));
                    var isAnonymous = false;
                    var regExpAnonymous = /_anonymous_/;
                    if (regExpAnonymous.test(n)) {
                        n = "Anonymous";
                        isAnonymous = true;
                    }
                    return { name: n,
                        anonymous: isAnonymous,
                        avatarUrl: DEFAULT_AVATAR_URL };
                };
                SwellRTService.prototype.open = function (_id) {
                    // Add the domain part to the Id
                    if (_id.indexOf("/") == -1) {
                        _id = SwellRT.domain() + "/" + _id;
                    }
                    this.object = new Promise(function (resolve, reject) {
                        try {
                            SwellRT.openModel(_id, function (object) {
                                resolve(object);
                            });
                        }
                        catch (e) {
                            reject(e);
                        }
                    });
                    return this.object;
                };
                SwellRTService.prototype.get = function () {
                    return this.object;
                };
                SwellRTService.prototype.close = function () {
                    this.object.then(function (object) {
                        SwellRT.closeModel(object.id());
                    });
                    this.object = new Promise(function (resolve, reject) {
                        reject();
                    });
                    return this.object;
                };
                SwellRTService.prototype.editor = function (parentElementId) {
                    return SwellRT.editor(parentElementId);
                };
                SwellRTService = __decorate([
                    core_1.Injectable(), 
                    __metadata('design:paramtypes', [])
                ], SwellRTService);
                return SwellRTService;
            }());
            exports_1("SwellRTService", SwellRTService);
        }
    }
});
//# sourceMappingURL=swellrt.service.js.map