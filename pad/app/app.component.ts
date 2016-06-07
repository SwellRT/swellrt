
import { Component } from 'angular2/core';
import { OnInit } from 'angular2/core';
import { LandingComponent } from './landing.component';
import { UserSpaceComponent } from './user-space/user-space.component';
import { EditorComponent } from './editor/editor.component';
import { RouteConfig, ROUTER_DIRECTIVES, ROUTER_PROVIDERS } from 'angular2/router';
import { SwellRTService } from './service/swellrt.service';

@Component({
  selector: 'app',
  template: `
    <div class="container">

      <nav class="navbar navbar-default">
        <div class="container-fluid">
          <div class="navbar-header">
            <a class="navbar-brand" href="#">SwellRT Editor</a>
          </div>
        </div>
      </nav>

      <router-outlet></router-outlet>

      <div id="snackbar-container"></div>
    </div>
  `,
  directives: [ROUTER_DIRECTIVES],
  providers: [ROUTER_PROVIDERS, SwellRTService]
})

@RouteConfig([

  // When your are anonymous
  { path: '/', name: 'Landing', component: LandingComponent, useAsDefault: true },

  { // User Space
    path: '/user',
    name: 'UserSpace',
    component: UserSpaceComponent
  },

  { // Document Editor
    path: '/edit/:id',
    name: 'EditDocument',
    component: EditorComponent
  }

])


export class AppComponent implements OnInit {

  constructor(private _swellrt: SwellRTService) {
  }

  ngOnInit() {
    this._swellrt.bindListeners();
    this._swellrt.resume(true);
  }

}
