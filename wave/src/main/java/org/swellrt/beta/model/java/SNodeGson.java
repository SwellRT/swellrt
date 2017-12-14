package org.swellrt.beta.model.java;

import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SPrimitive;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class SNodeGson {


  public static SNode castToSNode(JsonElement object) {

    Preconditions.checkArgument(object != null, "Null argument");

    if (object.isJsonPrimitive()) {

      JsonPrimitive primitiveObject = object.getAsJsonPrimitive();

      if (primitiveObject.isBoolean()) {

        return new SPrimitive(primitiveObject.getAsBoolean(), new SNodeAccessControl());

      } else if (primitiveObject.isNumber()) {

        return new SPrimitive(primitiveObject.getAsDouble(), new SNodeAccessControl());

      } else if (primitiveObject.isString()) {

        return new SPrimitive(primitiveObject.isString(), new SNodeAccessControl());

      }

    } else if (object.isJsonObject()) {

      return SMapGson.create(object.getAsJsonObject());

    } else if (object.isJsonArray()) {

      return SListGson.create(object.getAsJsonArray());

    }

    throw new IllegalStateException("Unable to cast object to JS native SNode");
  }

}
