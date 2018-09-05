package org.swellrt.beta.model.java;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.ModelFactory;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SNodeBuilder;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SViewBuilder;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.wave.Blip;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;


public class JavaModelFactory extends ModelFactory {

  static {
    ModelFactory.instance = new JavaModelFactory();
  }

  @Override
  public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip,
      InteractiveDocument doc) {
    return new STextJavaWave(nodeManager, substrateId, blip);
  }

  @Override
  public SText createLocalText(String text) throws SException {
    return STextJavaLocal.create(text);
  }

  private final SNodeBuilder snodeBuilder = new SNodeBuilderJava();

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

  @Override
  public SNodeBuilder getSNodeBuilder() {
    return snodeBuilder;
  }

  @Override
  public SViewBuilder getJsonBuilder(SNode node) {
    return new SViewBuilderJava<SNode>(node);
  }
}
