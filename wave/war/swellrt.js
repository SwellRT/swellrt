
// SwellRT bootstrap script

// A fake SwellRT object to register on ready handlers
// before the GWT module is loaded
window.swell = {

  ready: function(handler) {
       if (!handler || typeof handler !== "function")
         return;

       if (window.swellrt.runtime) {
          handler(window.swellrt.runtime.get());
       } else {
          if (!window._lh)
            window._lh = [];
          window._lh.push(handler);
       }
    }
}

// Some alias
window.swellrt = window.swell;
window.swell.onReady = window.swell.ready;

var scripts = document.getElementsByTagName('script');
var thisScript = scripts[scripts.length -1];


if (thisScript) {
  var p = document.createElement('a');
  p.href = thisScript.src;

  // Polyfill for ES6 proxy/reflect
  if (!window.Proxy || !window.Reflect) {
    try {
      var reflectSrc = p.protocol + "//" +p.host  + "/reflect.js";
      document.write("<script src='"+reflectSrc+"'></script>");
    } catch (e) {
      console.log("No proxies supported: "+e);
    }
  }


  var scriptSrc = p.protocol + "//" +p.host  + "/swellrt_beta/swellrt_beta.nocache.js";
  document.write("<script src='"+scriptSrc+"'></script>");
} else {
  console.log("Unable to inject swellrt script!");
}
