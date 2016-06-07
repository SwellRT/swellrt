System.register(['angular2/core', 'angular2/router', './user-panel.component', './service/swellrt.service'], function(exports_1, context_1) {
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
    var core_1, router_1, user_panel_component_1, swellrt_service_1;
    var LandingComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (router_1_1) {
                router_1 = router_1_1;
            },
            function (user_panel_component_1_1) {
                user_panel_component_1 = user_panel_component_1_1;
            },
            function (swellrt_service_1_1) {
                swellrt_service_1 = swellrt_service_1_1;
            }],
        execute: function() {
            LandingComponent = (function () {
                function LandingComponent(_swellrt, _router) {
                    this._swellrt = _swellrt;
                    this._router = _router;
                    this.wasError = false;
                }
                LandingComponent.prototype.createDocument = function () {
                    var randomId = btoa(Math.random().toString(36)).replace(/=/g, '');
                    this.openDocument(randomId);
                };
                LandingComponent.prototype.openDocument = function (_id) {
                    if (!_id) {
                        this.msgError = 'Write a name for the pad.';
                        this.wasError = true;
                        return;
                    }
                    this.wasError = false;
                    var link = ['EditDocument', { id: _id }];
                    this._router.navigate(link);
                };
                LandingComponent = __decorate([
                    core_1.Component({
                        selector: 'landing',
                        template: "\n\n\n    <div class=\"row\">\n\n      <div class=\"col-md-3\">\n        <user-panel #userPanel></user-panel>\n      </div>\n\n      <div class=\"col-md-6 col-md-offset-1\">\n\n        <div class=\"alert alert-dismissible alert-danger\" *ngIf=\"wasError\">\n          <button type=\"button\" class=\"close\" data-dismiss=\"alert\" (click)=\"wasError = false\">\u00D7</button>\n          <strong>{{msgError}}</strong>\n        </div>\n\n        <div class=\"panel panel-default\">\n          <div class=\"panel-body\">\n            <a (click)=\"createDocument()\"><h3>Create a new Document</h3></a>\n            <p>\n              Write a document. Share and edit it with others.\n            </p>\n          </div>\n        </div>\n\n        <div class=\"panel panel-default\">\n          <form class=\"panel-body\" (ngSubmit)=\"openDocument(documentId);\">\n            <a (click)=\"openDocument(documentId)\"><h3>Open a Document</h3></a>\n            <p>\n              Do you have a shared document ID? Use it to open the document again...\n            </p>\n\n            <div class=\"form-group label-floating\">\n              <label class=\"control-label\" for=\"documentIdInput\">Document ID here</label>\n              <input [(ngModel)]=\"documentId\" class=\"form-control\" id=\"documentIdInput\" type=\"text\">\n            </div>\n\n            <button class=\"btn btn-primary pull-right\">Open</button>\n          </form>\n        </div>\n\n        <div class=\"panel panel-default\">\n          <div class=\"panel-body\">\n            <a (click)=\"userPanel.panelState = 'collapsed'\"><h3>Sign up</h3></a>\n            <p>\n              Take advantage of being a registered user of SwellRT Editor.\n              Manage all the documents you collaborate with on the cloud.\n            </p>\n          </div>\n        </div>\n\n      </div>\n\n    </div>\n    ",
                        directives: [user_panel_component_1.UserPanelComponent]
                    }), 
                    __metadata('design:paramtypes', [swellrt_service_1.SwellRTService, router_1.Router])
                ], LandingComponent);
                return LandingComponent;
            }());
            exports_1("LandingComponent", LandingComponent);
        }
    }
});
//# sourceMappingURL=landing.component.js.map