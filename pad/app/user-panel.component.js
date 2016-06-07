System.register(['angular2/core', './service/swellrt.service', 'angular2/router'], function(exports_1, context_1) {
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
    var core_1, swellrt_service_1, router_1;
    var UserPanelComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (swellrt_service_1_1) {
                swellrt_service_1 = swellrt_service_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            }],
        execute: function() {
            UserPanelComponent = (function () {
                function UserPanelComponent(_router, _swellrt) {
                    this._router = _router;
                    this._swellrt = _swellrt;
                    this.panelState = "collapsed";
                }
                UserPanelComponent.prototype.ngOnInit = function () {
                    var _this = this;
                    this._swellrt.getUser().then(function (user) {
                        _this.loggedInUser = user;
                    });
                };
                UserPanelComponent.prototype.clearForms = function () {
                    this.nameInput = null;
                    this.passwordInput = null;
                    this.repasswordInput = null;
                };
                UserPanelComponent.prototype.login = function () {
                    var _this = this;
                    this.panelState = "collapsed";
                    this._swellrt.login(this.nameInput + this._swellrt.domain, this.passwordInput).then(function (user) {
                        _this.loggedInUser = user;
                        _this.clearForms();
                    });
                };
                UserPanelComponent.prototype.create = function () {
                    var _this = this;
                    this.panelState = "collapsed";
                    this._swellrt.createUser(this.nameInput, this.passwordInput).then(function () {
                        return _this._swellrt.login(_this.nameInput, _this.passwordInput);
                    }).then(function (user) {
                        _this.loggedInUser = user;
                        _this.clearForms();
                    });
                };
                UserPanelComponent.prototype.logout = function () {
                    var _this = this;
                    this._swellrt.logout(true).then(function (user) { _this.loggedInUser = user; });
                };
                UserPanelComponent.prototype.showLoginForm = function () {
                    this.panelState = "loginForm";
                };
                UserPanelComponent.prototype.showRegisterForm = function () {
                    this.panelState = "registerForm";
                };
                UserPanelComponent.prototype.cancelForm = function () {
                    this.panelState = "collapsed";
                };
                UserPanelComponent = __decorate([
                    core_1.Component({
                        selector: 'user-panel',
                        template: "\n\n      <div class=\"panel panel-default\" *ngIf=\"loggedInUser\">\n        <div class=\"panel-body\">\n\n          <!-- Logged In user -->\n          <div class=\"media\" *ngIf=\"!loggedInUser.anonymous\">\n            <div class=\"media-left\">\n              <a href=\"#\">\n                <img class=\"media-object\" height=\"40\" src=\"{{loggedInUser.avatarUrl}}\" alt=\"\">\n              </a>\n            </div>\n            <div class=\"media-body\">\n              <h5 class=\"media-heading\">{{loggedInUser.name}}</h5>\n              <span *ngIf=\"panelState == 'collapsed'\">\n                <a href=\"javascript:void(0)\" (click)=\"logout()\">Logout</a>\n              </span>\n            </div>\n          </div>\n\n\n          <!-- Not Logged In user -->\n          <div class=\"media\" *ngIf=\"loggedInUser.anonymous\">\n            <div class=\"media-left media-middle\">\n              <a href=\"#\">\n                <img class=\"media-object\" height=\"40\" src=\"images/anonymous.png\" alt=\"\">\n              </a>\n            </div>\n            <div class=\"media-body\">\n              <h5 class=\"media-heading\">Anonymous</h5>\n              <span *ngIf=\"panelState == 'collapsed'\">\n                <a href=\"javascript:void(0)\" (click)=\"showLoginForm()\">Login &nbsp;|</a>&nbsp;&nbsp;<a href=\"javascript:void(0)\" (click)=\"showRegisterForm()\">Create account</a>\n              </span>\n            </div>\n          </div>\n\n\n          <form style=\"margin-top:4em\" *ngIf=\"panelState == 'loginForm'\" (ngSubmit)=\"login()\">\n\n            <div class=\"form-group label-floating\">\n              <label class=\"control-label\" for=\"loginNameInput\">Name</label>\n              <input class=\"form-control\" id=\"loginNameInput\" type=\"text\" [(ngModel)]=\"nameInput\">\n            </div>\n            <div class=\"form-group label-floating\">\n              <label class=\"control-label\" for=\"loginPasswordInput\">Password</label>\n              <input class=\"form-control\" id=\"loginPasswordInput\" type=\"password\" [(ngModel)]=\"passwordInput\">\n            </div>\n\n            <a class=\"btn btn-default\" (click)=\"cancelForm()\">Cancel</a>\n            <button class=\"btn btn-primary\">Login</button>\n          </form>\n\n\n          <form style=\"margin-top:4em\" *ngIf=\"panelState == 'registerForm'\" (ngSubmit)=\"create()\">\n\n            <div class=\"form-group label-floating\">\n              <label class=\"control-label\" for=\"registerNameInput\">Name</label>\n              <input class=\"form-control\" id=\"registerNameInput\" type=\"text\" [(ngModel)]=\"nameInput\">\n            </div>\n            <div class=\"form-group label-floating\">\n              <label class=\"control-label\" for=\"registerPasswordInput\">Password</label>\n              <input class=\"form-control\" id=\"registerPasswordInput\" type=\"password\" [(ngModel)]=\"passwordInput\">\n            </div>\n            <div class=\"form-group label-floating\">\n              <label class=\"control-label\" for=\"registerRepasswordInput\">Repeat Password</label>\n              <input class=\"form-control\" id=\"registerRepasswordInput\" type=\"password\" [(ngModel)]=\"repasswordInput\">\n            </div>\n\n            <a class=\"btn btn-default\" (click)=\"cancelForm()\">Cancel</a>\n            <button class=\"btn btn-primary\">Create</button>\n          </form>\n\n\n        </div><!-- panel-body -->\n      </div><!-- panel -->\n\n\n      <div class=\"panel panel-default\" *ngIf=\"!loggedInUser\">\n        <div class=\"panel-body\">\n          Loading...\n        </div>\n      </div>\n    "
                    }), 
                    __metadata('design:paramtypes', [router_1.Router, swellrt_service_1.SwellRTService])
                ], UserPanelComponent);
                return UserPanelComponent;
            }());
            exports_1("UserPanelComponent", UserPanelComponent);
        }
    }
});
//# sourceMappingURL=user-panel.component.js.map