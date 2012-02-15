/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.box.waveimport.google;

// Import order matters here due to what looks like a javac bug.
// Eclipse doesn't seem to have this problem.
import org.waveprotocol.wave.communication.gson.GsonSerializable;
import org.waveprotocol.wave.communication.gson.GsonException;
import org.waveprotocol.wave.communication.gson.GsonUtil;
import org.waveprotocol.wave.communication.json.RawStringData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Pojo implementation of RobotSearchDigest with gson serialization and deserialization.
 *
 * Generated from google-import.proto. Do not edit.
 */
public final class RobotSearchDigestGsonImpl extends RobotSearchDigestImpl
    implements GsonSerializable {
  public RobotSearchDigestGsonImpl() {
    super();
  }

  public RobotSearchDigestGsonImpl(RobotSearchDigest message) {
    super(message);
  }

  @Override
  public JsonElement toGson(RawStringData raw, Gson gson) {
    return toGsonHelper(this, raw, gson);
  }

  /**
   * Static implementation-independent GSON serializer. Call this from
   * {@link #toGson} to avoid subclassing issues with inner message types.
   */
  public static JsonElement toGsonHelper(RobotSearchDigest message, RawStringData raw, Gson gson) {
    JsonObject json = new JsonObject();
    json.add("1", new JsonPrimitive(message.getWaveId()));
    {
      JsonArray array = new JsonArray();
      for (int i = 0; i < message.getParticipantSize(); i++) {
        array.add(new JsonPrimitive(message.getParticipant(i)));
      }
      json.add("2", array);
    }
    json.add("3", new JsonPrimitive(message.getTitle()));
    json.add("4", new JsonPrimitive(message.getSnippet()));
    json.add("5", GsonUtil.toJson(message.getLastModifiedMillis()));
    json.add("6", new JsonPrimitive(message.getBlipCount()));
    json.add("7", new JsonPrimitive(message.getUnreadBlipCount()));
    return json;
  }

  @Override
  public void fromGson(JsonElement json, Gson gson, RawStringData raw) throws GsonException {
    reset();
    JsonObject jsonObject = json.getAsJsonObject();
    // NOTE: always check with has(...) as the json might not have all required
    // fields set.
    if (jsonObject.has("1")) {
      setWaveId(jsonObject.get("1").getAsString());
    }
    if (jsonObject.has("2")) {
      JsonArray array = jsonObject.get("2").getAsJsonArray();
      for (int i = 0; i < array.size(); i++) {
        addParticipant(array.get(i).getAsString());
      }
    }
    if (jsonObject.has("3")) {
      setTitle(jsonObject.get("3").getAsString());
    }
    if (jsonObject.has("4")) {
      setSnippet(jsonObject.get("4").getAsString());
    }
    if (jsonObject.has("5")) {
      setLastModifiedMillis(GsonUtil.fromJson(jsonObject.get("5")));
    }
    if (jsonObject.has("6")) {
      setBlipCount(jsonObject.get("6").getAsInt());
    }
    if (jsonObject.has("7")) {
      setUnreadBlipCount(jsonObject.get("7").getAsInt());
    }
  }

}