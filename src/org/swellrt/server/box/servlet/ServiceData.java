package org.swellrt.server.box.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;


public abstract class ServiceData {

  private static JsonParser jsonParser = new JsonParser();
  private static Gson gson = new Gson();


  public static ServiceData fromJson(String json, Class<? extends ServiceData> classOf)
      throws JsonParseException {

    ServiceData object = null;


    JsonElement element = jsonParser.parse(json);

    if (element == null) throw new JsonParseException("Element is null");

    object = gson.fromJson(element, classOf);
    if (element.isJsonObject()) object.set(element.getAsJsonObject());


    return object;
  }


  public static ServiceData[] arrayFromJson(String json, Class<? extends ServiceData[]> classOf)
      throws JsonParseException {

    ServiceData[] object = null;


    JsonElement element = jsonParser.parse(json);

    if (element == null) throw new JsonParseException("Element is null");

    object = gson.fromJson(element, classOf);
    JsonArray array = element.getAsJsonArray();
    for (int i = 0; i < object.length && i < array.size(); i++) {
      object[i].set(array.get(i).getAsJsonObject());
    }


    return object;
  }

  /**
   * The original raw JsonObject
   */
  private JsonObject json;

  public ServiceData() {
    this.json = null;
  }

  private void set(JsonObject json) {
    this.json = json;
  }

  /**
   * Check if a field of this object was set in the original Json
   * @param field
   * @return true if the field was in the original Json
   */
  public boolean isParsedField(String field) {
    return json.has(field);

  }

  public String toJson() {
    return gson.toJson(this);
  }

}
