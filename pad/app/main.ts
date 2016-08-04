import {bootstrap}    from '@angular/platform-browser-dynamic';
import { disableDeprecatedForms, provideForms } from '@angular/forms';
import {AppComponent} from './app.component';
import {ROUTER_PROVIDERS} from '@angular/router-deprecated';
import {enableProdMode} from '@angular/core';

enableProdMode();
bootstrap(AppComponent, [ROUTER_PROVIDERS,  disableDeprecatedForms(),
  provideForms()]);
