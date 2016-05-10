package org.swellrt.web;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * The JavaScript Web API for SwellRT.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WebAPI extends JavaScriptObject {

  public static final String PROP_SERVICE_CONTEXT  = "/swell";

  protected WebAPI() {

  }


  public static WebAPI create(String server){

    WebAPI wapi = (WebAPI) JavaScriptObject.createObject();
    wapi.setupHttp(server, PROP_SERVICE_CONTEXT);
    wapi.setupLoginMethods();
    return wapi;

  }

  protected final native WebAPI setupHttp(String serverUrl, String serviceContext) /*-{

       this._http = {

        server: serverUrl,

        context: serviceContext,

        //
        // Makes a HTTP call with JSON for request and response.
        // Sets all the required headers for the SwellRT server.
        //
        // Returns a promise.
        //
        call: function(method, op, payload) {

          var url = this.server + this.context + op;
          var p = new Promise(function(resolve, reject) {

            try {

              var r = new XMLHttpRequest();
              r.open(method, url);

              r.withCredentials = true;
              r.setRequestHeader("Content-Type", "text/plain; charset=utf-8");

              if ($wnd.sessionStorage) {
                r.setRequestHeader("X-window-id",  $wnd.sessionStorage.getItem("x-swellrt-window-id"));
              }

              r.onreadystatechange = function() {
                  if (r.readyState === 4) {

                    if (r.status === 200) {

                      var type = r.getResponseHeader("Content-Type");
                      resolve(JSON.parse(r.responseText));

                    } else {

                      var error = {
                          status: r.statusText,
                          code: r.status,
                          response: null,
                      };

                      var type = r.getResponseHeader("Content-Type");
                      error.response = JSON.parse(r.responseText);
                      reject(error);

                    }

                  }
               }

              r.send(JSON.stringify(payload));


            } catch(e) {
              reject(e);
            }

          });
         return p;
      }


      }

    return this;

  }-*/;



  protected final native void setupLoginMethods()  /*-{

    var _session = this._session = new Object();
    var _http = this._http;

    this.login = function(credentials) {

      var p = new Promise(function(resolve, reject) {

          _http.call("POST", "/auth", credentials)

            .then(function(response) {
             _session = response;
             resolve(_session);

            })['catch'](function(error) {
              reject(error);
          });

      });

      return p;
    };


    this.logout = function() {

      var p = new Promise(function(resolve, reject) {

        _http.call("POST", "/auth", {})

          .then(function(response) {
             _session = response;
             resolve(_session);

          })['catch'](function(error){
            reject(error);
          });

      });

      return p;
    };


  }-*/;

}
