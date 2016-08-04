import {Component} from '@angular/core';


@Component({
    selector: 'app-user-space',
    template: `

        <div class="row">

          <!-- Left bar -->
          <div class="col-md-4">
            <p>Here User Widget</p>
          </div>

          <!-- Main area -->
          <div class="col-md-8">

            <!-- Space Navigation & Title Area -->
            <div class="row">
              <h3>Your Documents</h3>
            </div>

            <!-- Space Content Area -->
            <div class="row">
              <p>Here document listing</p>
            </div>

          </div>

        </div>

    `
  })



export class UserSpaceComponent {

  constructor() { }

}
