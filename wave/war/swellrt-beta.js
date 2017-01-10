
// SwellRT bootstrap script

// A fake SwellRT object to register on ready handlers
// before the GWT module is loaded

window._lh = [];

window.swellrt = {

  onReady: function(handler) {
       if (!handler || typeof handler !== "function")
         return;

       _lh.push(handler);
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


  var scriptSrc = p.protocol + "//" +p.host  + "/swellrt_beta/swellrt_beta.nocache.js";
  document.write("<script src='"+scriptSrc+"'></script>");
}

