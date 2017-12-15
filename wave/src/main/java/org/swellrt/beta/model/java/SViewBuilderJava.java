package org.swellrt.beta.model.java;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SViewBuilder;
import org.swellrt.beta.model.SVisitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A visitor to build a JSON view from a SwellRT {@SNode}
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 * @param <T>
 */
public class SViewBuilderJava<T extends SNode> implements SViewBuilder, SVisitor<T> {

  JsonElement currentObject;
  Object primitiveObject = null;;
  SException ex;
  final T root;

  public SViewBuilderJava(T root) {
    this.root = root;
  }

  @Override
  public void visit(SPrimitive primitive) {
    currentObject = new JsonPrimitive(primitive.asString());
    primitiveObject = primitive.getValue();
  }

  @Override
  public void visit(SMap map) {

    JsonObject jsonObject = new JsonObject();

    try {

      for (String key : map.keys()) {
        map.pick(key).accept(this);
        jsonObject.add(key, currentObject);
      }

    } catch (SException e) {
      ex = e;
      return;
    }

    currentObject = jsonObject;
  }

  @Override
  public void visit(SList<T> list) {

    JsonArray jsonArray = new JsonArray();

    for (int i = 0; i < list.size(); i++) {
      try {
        list.pick(i).accept(this);
        jsonArray.add(currentObject);
      } catch (SException e) {
        ex = e;
        return;
      }
    }

    currentObject = jsonArray;

  }


  public void visit(SNode node) {

    if (node instanceof SMap || node instanceof SObject) {
      visit((SMap) node);
      return;
    }

    if (node instanceof SList) {
      visit(node);
      return;
    }

    if (node instanceof SPrimitive) {
      visit((SPrimitive) node);
      return;
    }

    if (node instanceof SText) {
      visit((SText) node);
      return;
    }

  }


  @Override
  public void visit(SText text) {
    currentObject = new JsonPrimitive(text.getRawContent());
  }

  public Object build() throws SException {

    currentObject = null;
    ex = null;

    visit(root);

    if (ex != null)
      throw ex;

    // Return the primitive value as sugar syntax in java
    if (currentObject.isJsonPrimitive())
      return primitiveObject;

    return currentObject;

  }


}
