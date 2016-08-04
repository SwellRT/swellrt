import { Component, OnInit } from '@angular/core';
import { UserPanelComponent } from '../user-panel.component';
import { RouteParams } from '@angular/router-deprecated';
import { SwellRTService } from '../service/swellrt.service';

@Component({
    selector: 'app-editor',
    templateUrl: 'app/editor/editor.component.html',
    directives: [UserPanelComponent]
  })

export class EditorComponent implements OnInit {

  _title: any;
  editor: any;

  wasError: boolean = false;
  msgError: string;

  formats: Array<Array<string>> = [
    ['bold', 'italic', 'underline', 'strikethrough'],
    // ['size', 'color_text', 'color_fill'],
    ['align_left', 'align_center', 'align_right'],
    ['list_bulleted', 'list_numbered']
  ];

  annotations: Array<any>;

  annotationMap = {
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

  buttons: Map<string, boolean> = new Map<string, boolean>();

  constructor(private _swellrt: SwellRTService,
    private _routeParams: RouteParams) {
      this.disableAllButtons();
  }

  get editorElem() {
    return (<HTMLElement>document.querySelector('#editor-container > div'));
  }

  get title() {
    return this._title && this._title.getValue();
  }

  set title(value) {
    if (this._title) {
      this._title.setValue(value);
    }
  }

  updateAllButtons() {
    for (let formatGroup of this.formats) {
      for (let format of formatGroup) {
        let [key, val] = this.annotationMap[format].split('=');
        this.buttons[format] = this.annotations && (this.annotations[key] === val);
      }
    }
  }

  disableAllButtons() {
    for (let formatGroup of this.formats) {
      for (let format of formatGroup) {
        this.buttons[format] = false;
      }
    }
  }

  ngOnInit() {

    let widgets = {

      'img-link' : {

        onInit: function(parentElement, state) {
          parentElement.innerHTML = `<img src="${state}">`;
        },

        onChangeState: function(parentElement, before, state) {
          parentElement.innerHTML = `<img src="${state}">`;
        }
      }

    };

    let annotations =  {};

    this.editor = this._swellrt.editor('editor-container', widgets, annotations);

    this._swellrt.getUser().then(user => {

      let id = this._routeParams.get('id');
      this._swellrt.open(id).then(cObject => {

        cObject.addParticipant(this._swellrt.domain);

        // Initialize the doc
        if (!cObject.root.get('doc')) {
          cObject.root.put('doc', cObject.createText(''));
        }

        // Initialize the doc's title
        if (!cObject.root.get('doc-title')) {
          cObject.root.put('doc-title', cObject.createString('New document'));
        }

        // Open the doc in the editor
        this._title = cObject.root.get('doc-title');
        this.editor.edit(cObject.root.get('doc'));

        this.editor.onSelectionChanged((annotations) => {
          this.annotations = annotations;
          this.updateAllButtons();
        });

        this.editorElem.addEventListener('focus', () => this.updateAllButtons());
        this.editorElem.addEventListener('blur', () => this.disableAllButtons());

      })
      .catch(error => {
        this.wasError = true;
        this.msgError = `Document doesn't exist or you don't have permission to open (${ error })`;
      });

    }).catch( error => {
      this.wasError = true;
      this.msgError = 'There is any session open.';
    });

  }

  annotate (format) {
    let [key, val] = this.annotationMap[format].split('=');
    let currentVal = this.annotations[key];
    if (currentVal === val) {
      val = null;
    }

    this.annotations[key] = val;
    this.editor.setAnnotation(key, val);
    this.editorElem.focus();
  }

  addImage (file) {
    let img = prompt('Image URL', 'http://lorempixel.com/600/600/');
    if (img) {
      this.editor.addWidget('img-link', img);
    }
  }
}
