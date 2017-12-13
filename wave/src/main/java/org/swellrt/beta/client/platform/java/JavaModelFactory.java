package org.swellrt.beta.client.platform.java;

import org.swellrt.beta.common.ModelFactory;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.wave.Blip;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class JavaModelFactory extends ModelFactory {

  @Override
  public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip,
      InteractiveDocument doc) {

    return new SWaveText(nodeManager, substrateId, blip) {

      @Override
      public String getRawContent() {
        return blip.getContent().toXmlString();
      }

      @Override
      public DocInitialization getInitContent() {
        return blip.getContent().toInitialization();
      }

      @Override
      public void setInitContent(DocInitialization ops) {
        throw new IllegalStateException("Not implemented");
      }

      @Override
      public boolean isEmpty() {
        return SUtils.isEmptyDocument(blip.getContent());
      }

      @Override
      public void accept(SVisitor visitor) {
        visitor.visit(this);
      }

      @Override
      public void set(String path, Object value) {
      }

      @Override
      public Object get(String path) {
        return null;
      }

      @Override
      public void push(String path, Object value) {
      }

      @Override
      public Object pop(String path) {
        return null;
      }

      @Override
      public void delete(String path) {
      }

      @Override
      public int length(String path) {
        return 0;
      }

      @Override
      public boolean contains(String path, String property) {
        return false;
      }

      @Override
      public SNode node(String path) {
        return null;
      }

      @Override
      public SMap asMap() {
        return null;
      }

      @Override
      public SList<? extends SNode> asList() {
        return null;
      }

      @Override
      public String asString() {
        return null;
      }

      @Override
      public double asDouble() {
        return 0;
      }

      @Override
      public int asInt() {
        return 0;
      }

      @Override
      public boolean asBoolean() {
        return false;
      }

      @Override
      public SText asText() {
        return null;
      }

    };

  }

  @Override
  public STextLocal createLocalText(String text) throws SException {
    throw new IllegalStateException("Not implemented yet");
  }

  Gson gson = new Gson();
  JsonParser jsonParser = new JsonParser();

  @Override
  public boolean isJsonObject(Object o) {
    return o != null && o instanceof JsonElement;
  }

  @Override
  public Object parseJsonObject(String json) {
    return jsonParser.parse(json);
  }

  @Override
  public String serializeJsonObject(Object o) {
    return gson.toJson((JsonElement) o);
  }

  @Override
  public Object traverseJsonObject(Object o, String path) {

    if (o == null)
      return null;

    JsonElement e = (JsonElement) o;

    if (path == null || path.isEmpty()) {

      if (e.isJsonPrimitive()) {
        JsonPrimitive p = (JsonPrimitive) e;

        if (p.isBoolean())
          return new Boolean(p.getAsBoolean());
        else if (p.isNumber())
          return new Double(p.getAsDouble());
        else if (p.isString())
          return new String(p.getAsString());
        else
          return null;

      } else
        return e;
    }

    String propName = path.indexOf(".") != -1 ? path.substring(0, path.indexOf(".")) : path;
    String restPath = path.indexOf(".") != -1 ? path.substring(path.indexOf(".") + 1) : null;

    JsonElement propValue = null;
    if (e.isJsonObject()) {

      JsonObject object = (JsonObject) e;
      propValue = object.get(propName);
      return traverseJsonObject(propValue, restPath);

    } else if (e.isJsonArray()) {
      try {
        int index = Integer.parseInt(propName);
        JsonArray array = (JsonArray) e;
        return traverseJsonObject(array.get(index), restPath);
      } catch (NumberFormatException ex) {
        return null;
      }

    } else if (e.isJsonPrimitive()) {
      return null;
    }


    return null;
  }
}
