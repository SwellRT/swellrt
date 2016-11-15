
// SwellRT bootstrap script

// A fake SwellRT object to register on ready handlers
// before the GWT module is loaded


window.SwellRT = {

  _readyHandlers: [],

  ready: function(handler) {
       if (!handler || typeof handler !== "function")
         return;

       this._readyHandlers.push(handler);
      }
}

var scripts = document.getElementsByTagName('script');
var thisScript = scripts[scripts.length -1];





if (thisScript) {
  var p = document.createElement('a');
  p.href = thisScript.src;

  // Polyfill for ES6 proxy/reflect
  if (!window.Proxy || !window.Reflect) {
    var reflectSrc = p.protocol + "//" +p.host  + "/reflect.js";
    document.write("<script src='"+reflectSrc+"'></script>");
  }


  var scriptSrc = p.protocol + "//" +p.host  + "/webapi/webapi.nocache.js";
  document.write("<script src='"+scriptSrc+"'></script>");
}

