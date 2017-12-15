
// SwellRT bootstrap script

// A fake SwellRT object to register on ready handlers
// before the GWT module is loaded

var SWELL_CONTEXT = "swellrt_beta";
var SWELL_JS_MODULE = "swellrt_beta.nocache.js";

window.swell = {

  ready: function(handler) {
       if (!handler || typeof handler !== "function")
         return;

       if (window.swellrt.runtime) {
          handler(window.swell.runtime.get());
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

// Load Swell code

var scripts = document.getElementsByTagName('script');
var thisScript = scripts[scripts.length -1];


if (thisScript) {
	
  var fakeAnchor = document.createElement('a');
  fakeAnchor.href = thisScript.src;
  var scriptSrc = fakeAnchor.protocol + "//" + fakeAnchor.host + "/" + SWELL_CONTEXT + "/" + SWELL_JS_MODULE;
  
  var scriptTag = document.createElement('script');
  scriptTag.setAttribute("type","text/javascript");
  scriptTag.setAttribute("src", scriptSrc);
  scriptTag.setAttribute("async", true);

  thisScript.parentNode.appendChild(scriptTag);
  
  //document.getElementsByTagName("head")[0].appendChild(scriptTag);
  
} else {
  console.log("Error injecting Swell Javascript!");
}
