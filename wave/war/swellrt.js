
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
  var scriptSrc = p.protocol + "//" +p.host  + "/swellrt/swellrt.nocache.js";
  document.write("<script src='"+scriptSrc+"'></script>");
}

