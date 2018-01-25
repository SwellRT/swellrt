package org.swellrt.beta.client.platform.java;

import org.swellrt.beta.model.json.SJsonObject;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SJsonFactoryJava implements SJsonObject.Factory {

  JsonParser parser = new JsonParser();
  Gson gson = new Gson();


  @Override
  public SJsonObject create() {
    return new SJsonObjectJava();
  }

  @Override
  public SJsonObject parse(String json) {
    JsonElement jse = parser.parse(json);
    if (jse.isJsonObject()) {
      return new SJsonObjectJava(jse.getAsJsonObject());
    }
    return null;
  }

  @Override
  public String serialize(SJsonObject object) {
    if (object instanceof SJsonObjectJava) {
      SJsonObjectJava joj = (SJsonObjectJava) object;
      return gson.toJson(joj.getJsonObject());
    }
    return null;
  }

  @Override
  public SJsonObject create(Object jso) {
    if (jso instanceof JsonObject)
      return new SJsonObjectJava((JsonObject) jso);

    return null;
  }

}
