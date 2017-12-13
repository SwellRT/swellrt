package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.platform.web.editor.STextLocalWeb;
import org.swellrt.beta.client.platform.web.editor.STextRemoteWeb;
import org.swellrt.beta.common.ModelFactory;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.wave.Blip;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

public class WebModelFactory extends ModelFactory {

  @Override
  public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip,
      InteractiveDocument doc) {
    return new STextRemoteWeb(nodeManager, substrateId, blip, doc);

  }

  @Override
  public STextLocal createLocalText(String text) throws SException {
    return STextLocalWeb.create(text);
  }

  @Override
  public boolean isJsonObject(Object o) {
    return o != null && o instanceof JavaScriptObject;
  }

  @Override
  public Object parseJsonObject(String json) {
    if (json != null)
      return JsonUtils.<JavaScriptObject> safeEval(json);

    return null;
  }

  @Override
  public String serializeJsonObject(Object o) {
    return JsonUtils.stringify((JavaScriptObject) o);
  }


  private native boolean isJsArray(JavaScriptObject o) /*-{
    return Array.isArray(o);
  }-*/;

  private native boolean isJsObject(JavaScriptObject o) /*-{
    return typeof o === "object" && !Array.isArray(o);
  }-*/;

  private native Boolean isJsPrimitive(JavaScriptObject o) /*-{
     return typeof o == "number" || typeof o == "string" || typeof o == "boolean";
   }-*/;

   private native Double asNumber(JavaScriptObject o) /*-{
     if (typeof o == "number")
       return o;

     return null;
   }-*/;

   private native String asString(JavaScriptObject o) /*-{
     if (typeof o == "string")
       return o;

     return null;
   }-*/;

   private native Boolean asBoolean(JavaScriptObject o) /*-{
     if (typeof o == "boolean")
       return o;

     return null;
   }-*/;

  @Override
  public Object traverseJsonObject(Object o, String path) {

    if (o == null)
      return o;

    if (path == null || path.isEmpty()) {
      return o;
    }

    if (!(o instanceof JavaScriptObject))
      return null;

    JavaScriptObject jso = (JavaScriptObject) o;

    String propName = path.indexOf(".") != -1 ? path.substring(0, path.indexOf(".")) : path;
    String restPath = path.indexOf(".") != -1 ? path.substring(path.indexOf(".") + 1) : null;

    if (isJsObject(jso)) {

      JsoView jsv = JsoView.as(jso);
      return traverseJsonObject(jsv.getObjectUnsafe(propName), restPath);

    } else if (isJsArray(jso)) {

      try {
        int index = Integer.parseInt(propName);
        JsoView jsv = JsoView.as(jso);
        return traverseJsonObject(jsv.getJsoView(index), restPath);
      } catch (NumberFormatException ex) {
        return null;
      }

    }

    return null;
  }

}
