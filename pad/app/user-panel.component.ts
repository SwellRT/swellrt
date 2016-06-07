import { Component } from 'angular2/core';
import { OnInit } from 'angular2/core';
import { SwellRTService } from './service/swellrt.service';
import { User } from './data/user';
import { Router } from 'angular2/router';


@Component({
    selector: 'user-panel',
    template: `

      <div class="panel panel-default" *ngIf="loggedInUser">
        <div class="panel-body">

          <!-- Logged In user -->
          <div class="media" *ngIf="!loggedInUser.anonymous">
            <div class="media-left">
              <a href="#">
                <img class="media-object" height="40" src="{{loggedInUser.avatarUrl}}" alt="">
              </a>
            </div>
            <div class="media-body">
              <h5 class="media-heading">{{loggedInUser.name}}</h5>
              <span *ngIf="panelState == 'collapsed'">
                <a href="javascript:void(0)" (click)="logout()">Logout</a>
              </span>
            </div>
          </div>


          <!-- Not Logged In user -->
          <div class="media" *ngIf="loggedInUser.anonymous">
            <div class="media-left media-middle">
              <a href="#">
                <img class="media-object" height="40" src="images/anonymous.png" alt="">
              </a>
            </div>
            <div class="media-body">
              <h5 class="media-heading">Anonymous</h5>
              <span *ngIf="panelState == 'collapsed'">
                <a href="javascript:void(0)" (click)="showLoginForm()">Login &nbsp;|</a>&nbsp;&nbsp;<a href="javascript:void(0)" (click)="showRegisterForm()">Create account</a>
              </span>
            </div>
          </div>


          <div style="margin-top:4em" *ngIf="panelState == 'loginForm'" (ngSubmit)="login()">

            <div class="form-group label-floating">
              <label class="control-label" for="loginNameInput">Name</label>
              <input class="form-control" id="loginNameInput" type="text" [(ngModel)]="nameInput">
            </div>
            <div class="form-group label-floating">
              <label class="control-label" for="loginPasswordInput">Password</label>
              <input class="form-control" id="loginPasswordInput" type="password" [(ngModel)]="passwordInput">
            </div>

            <a class="btn btn-default" (click)="cancelForm()">Cancel</a>
            <button class="btn btn-primary">Login</button>
          </div>


          <form style="margin-top:4em" *ngIf="panelState == 'registerForm'" (ngSubmit)="create()">

            <div class="form-group label-floating">
              <label class="control-label" for="registerNameInput">Name</label>
              <input class="form-control" id="registerNameInput" type="text" [(ngModel)]="nameInput">
            </div>
            <div class="form-group label-floating">
              <label class="control-label" for="registerPasswordInput">Password</label>
              <input class="form-control" id="registerPasswordInput" type="password" [(ngModel)]="passwordInput">
            </div>
            <div class="form-group label-floating">
              <label class="control-label" for="registerRepasswordInput">Repeat Password</label>
              <input class="form-control" id="registerRepasswordInput" type="password" [(ngModel)]="repasswordInput">
            </div>

            <a class="btn btn-default" (click)="cancelForm()">Cancel</a>
            <button class="btn btn-primary">Create</button>
          </form>


        </div><!-- panel-body -->
      </div><!-- panel -->


      <div class="panel panel-default" *ngIf="!loggedInUser">
        <div class="panel-body">
          Loading...
        </div>
      </div>
    `
  })



export class UserPanelComponent implements OnInit {

  // The logged in user
  loggedInUser : User;
  // panelState = "collapsed | loginForm | registerForm"
  panelState : string;
  // Form fields
  nameInput : string;
  passwordInput : string;
  repasswordInput : string;


  constructor(private _router: Router,
    private _swellrt: SwellRTService) {
        this.panelState = "collapsed";
    }

  ngOnInit() {
    this._swellrt.getUser().then(user => {
        this.loggedInUser = user;
    });
  }


  clearForms() {
    this.nameInput = null;
    this.passwordInput = null;
    this.repasswordInput = null;
  }

  login() {
    this.panelState = "collapsed";
    this._swellrt.login(this.nameInput, this.passwordInput).then(
      user => {
        this.loggedInUser = user;
        this.clearForms();
      }
    );
  }

  create() {
    this._swellrt.createUser(this.nameInput, this.passwordInput).then(() => {
      this._swellrt.login(this.nameInput, this.passwordInput);
    })
  }

  logout() {
    this._swellrt.logout(true).then(user =>
      { this.loggedInUser = undefined; }
    );
  }

  showLoginForm() {
    this.panelState = "loginForm";
  }

  showRegisterForm() {
    this.panelState = "registerForm";
  }

  cancelForm() {
    this.panelState = "collapsed";
  }

}
