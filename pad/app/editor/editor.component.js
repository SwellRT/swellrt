System.register(['@angular/core', '../user-panel.component', '@angular/router-deprecated', '../service/swellrt.service'], function(exports_1, context_1) {
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
    var core_1, user_panel_component_1, router_deprecated_1, swellrt_service_1;
    var EditorComponent;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            },
            function (user_panel_component_1_1) {
                user_panel_component_1 = user_panel_component_1_1;
            },
            function (router_deprecated_1_1) {
                router_deprecated_1 = router_deprecated_1_1;
            },
            function (swellrt_service_1_1) {
                swellrt_service_1 = swellrt_service_1_1;
            }],
        execute: function() {
            EditorComponent = (function () {
                function EditorComponent(_swellrt, _routeParams) {
                    this._swellrt = _swellrt;
                    this._routeParams = _routeParams;
                    this.wasError = false;
                    this.formats = [
                        ['bold', 'italic', 'underline', 'strikethrough'],
                        // ['size', 'color_text', 'color_fill'],
                        ['align_left', 'align_center', 'align_right'],
                        ['list_bulleted', 'list_numbered']
                    ];
                    this.annotationMap = {
                        'bold': 'style/fontWeight=bold',
                        'italic': 'style/fontStyle=italic',
                        'underline': 'style/textDecoration=underline',
                        'strikethrough': 'style/textDecoration=line-through',
                        'align_left': 'paragraph/textAlign=left',
                        'align_center': 'paragraph/textAlign=center',
                        'align_right': 'paragraph/textAlign=right',
                        'list_bulleted': 'paragraph/listStyleType=unordered',
                        'list_numbered': 'paragraph/listStyleType=decimal'
                    };
                    this.buttons = new Map();
                    this.disableAllButtons();
                }
                Object.defineProperty(EditorComponent.prototype, "editorElem", {
                    get: function () {
                        return document.querySelector('#editor-container > div');
                    },
                    enumerable: true,
                    configurable: true
                });
                Object.defineProperty(EditorComponent.prototype, "title", {
                    get: function () {
                        return this._title && this._title.getValue();
                    },
                    set: function (value) {
                        if (this._title) {
                            this._title.setValue(value);
                        }
                    },
                    enumerable: true,
                    configurable: true
                });
                EditorComponent.prototype.updateAllButtons = function () {
                    for (var _i = 0, _a = this.formats; _i < _a.length; _i++) {
                        var formatGroup = _a[_i];
                        for (var _b = 0, formatGroup_1 = formatGroup; _b < formatGroup_1.length; _b++) {
                            var format = formatGroup_1[_b];
                            var _c = this.annotationMap[format].split('='), key = _c[0], val = _c[1];
                            this.buttons[format] = this.annotations && (this.annotations[key] === val);
                        }
                    }
                };
                EditorComponent.prototype.disableAllButtons = function () {
                    for (var _i = 0, _a = this.formats; _i < _a.length; _i++) {
                        var formatGroup = _a[_i];
                        for (var _b = 0, formatGroup_2 = formatGroup; _b < formatGroup_2.length; _b++) {
                            var format = formatGroup_2[_b];
                            this.buttons[format] = false;
                        }
                    }
                };
                EditorComponent.prototype.ngOnInit = function () {
                    var _this = this;
                    var widgets = {
                        'img-link': {
                            onInit: function (parentElement, state) {
                                parentElement.innerHTML = "<img src=\"" + state + "\">";
                            },
                            onChangeState: function (parentElement, before, state) {
                                parentElement.innerHTML = "<img src=\"" + state + "\">";
                            }
                        }
                    };
                    var annotations = {};
                    this.editor = this._swellrt.editor('editor-container', widgets, annotations);
                    this._swellrt.getUser().then(function (user) {
                        var id = _this._routeParams.get('id');
                        _this._swellrt.open(id).then(function (cObject) {
                            cObject.addParticipant(_this._swellrt.domain);
                            // Initialize the doc
                            if (!cObject.root.get('doc')) {
                                cObject.root.put('doc', cObject.createText(''));
                            }
                            // Initialize the doc's title
                            if (!cObject.root.get('doc-title')) {
                                cObject.root.put('doc-title', cObject.createString('New document'));
                            }
                            // Open the doc in the editor
                            _this._title = cObject.root.get('doc-title');
                            _this.editor.edit(cObject.root.get('doc'));
                            _this.editor.onSelectionChanged(function (annotations) {
                                _this.annotations = annotations;
                                _this.updateAllButtons();
                            });
                            _this.editorElem.addEventListener('focus', function () { return _this.updateAllButtons(); });
                            _this.editorElem.addEventListener('blur', function () { return _this.disableAllButtons(); });
                        })
                            .catch(function (error) {
                            _this.wasError = true;
                            _this.msgError = "Document doesn't exist or you don't have permission to open (" + error + ")";
                        });
                    }).catch(function (error) {
                        _this.wasError = true;
                        _this.msgError = 'There is any session open.';
                    });
                };
                EditorComponent.prototype.annotate = function (format) {
                    var _a = this.annotationMap[format].split('='), key = _a[0], val = _a[1];
                    var currentVal = this.annotations[key];
                    if (currentVal === val) {
                        val = null;
                    }
                    this.annotations[key] = val;
                    this.editor.setAnnotation(key, val);
                    this.editorElem.focus();
                };
                EditorComponent.prototype.addImage = function (file) {
                    var img = prompt('Image URL', 'http://lorempixel.com/600/600/');
                    if (img) {
                        this.editor.addWidget('img-link', img);
                    }
                };
                EditorComponent = __decorate([
                    core_1.Component({
                        selector: 'app-editor',
                        templateUrl: 'app/editor/editor.component.html',
                        directives: [user_panel_component_1.UserPanelComponent]
                    }), 
                    __metadata('design:paramtypes', [swellrt_service_1.SwellRTService, router_deprecated_1.RouteParams])
                ], EditorComponent);
                return EditorComponent;
            }());
            exports_1("EditorComponent", EditorComponent);
        }
    }
});
//# sourceMappingURL=editor.component.js.map