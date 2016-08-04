System.register(['@angular/core', './landing.component', './user-space/user-space.component', './editor/editor.component', '@angular/router-deprecated', './service/swellrt.service'], function(exports_1, context_1) {
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
    var core_1, landing_component_1, user_space_component_1, editor_component_1, router_deprecated_1, swellrt_service_1;
    var AppComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (landing_component_1_1) {
                landing_component_1 = landing_component_1_1;
            },
            function (user_space_component_1_1) {
                user_space_component_1 = user_space_component_1_1;
            },
            function (editor_component_1_1) {
                editor_component_1 = editor_component_1_1;
            },
            function (router_deprecated_1_1) {
                router_deprecated_1 = router_deprecated_1_1;
            },
            function (swellrt_service_1_1) {
                swellrt_service_1 = swellrt_service_1_1;
            }],
        execute: function() {
            AppComponent = (function () {
                function AppComponent(_swellrt) {
                    this._swellrt = _swellrt;
                }
                AppComponent.prototype.ngOnInit = function () {
                    this._swellrt.bindListeners();
                    this._swellrt.resume(true);
                };
                AppComponent = __decorate([
                    core_1.Component({
                        selector: 'app-root',
                        template: "\n    <div class=\"container\">\n\n      <nav class=\"navbar navbar-default\">\n        <div class=\"container-fluid\">\n          <div class=\"navbar-header\">\n            <a class=\"navbar-brand\" href=\"#\">SwellRT Editor</a>\n          </div>\n        </div>\n      </nav>\n\n      <router-outlet></router-outlet>\n\n      <div id=\"snackbar-container\"></div>\n    </div>\n  ",
                        directives: [router_deprecated_1.ROUTER_DIRECTIVES],
                        providers: [router_deprecated_1.ROUTER_PROVIDERS, swellrt_service_1.SwellRTService]
                    }),
                    router_deprecated_1.RouteConfig([
                        // When your are anonymous
                        { path: '/', name: 'Landing', component: landing_component_1.LandingComponent, useAsDefault: true },
                        {
                            path: '/user',
                            name: 'UserSpace',
                            component: user_space_component_1.UserSpaceComponent
                        },
                        {
                            path: '/edit/:id',
                            name: 'EditDocument',
                            component: editor_component_1.EditorComponent
                        }
                    ]), 
                    __metadata('design:paramtypes', [swellrt_service_1.SwellRTService])
                ], AppComponent);
                return AppComponent;
            }());
            exports_1("AppComponent", AppComponent);
        }
    }
});
//# sourceMappingURL=app.component.js.map