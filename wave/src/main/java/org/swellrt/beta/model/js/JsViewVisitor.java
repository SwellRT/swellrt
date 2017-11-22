package org.swellrt.beta.model.js;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SViewBuilder;
import org.swellrt.beta.model.SVisitor;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A visitor to build a JSON view from a SwellRT {@SNode}
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 * @param <T>
 */
public class JsViewVisitor<T extends SNode> implements SViewBuilder, SVisitor<T> {

  Object currentObject;
  SException ex;
  final T root;

  public JsViewVisitor(T root) {
    this.root = root;
  }

  @Override
  public void visit(SPrimitive primitive) {
    currentObject = primitive.getValue();
  }

  @Override
  public void visit(SMap map) {

    JsoView jso = JsoView.as(JsoView.createObject());

    try {

      for (String key : map.keys()) {
        map.pick(key).accept(this);
        jso.setObject(key, currentObject);
      }

    } catch (SException e) {
      ex = e;
      return;
    }

    currentObject = jso;
  }

  protected native void jsArrayPush(JavaScriptObject array, Object value) /*-{
    array.push(value);
  }-*/;

  @Override
  public void visit(SList<T> list) {

    JavaScriptObject jsarray = JsoView.createArray();

    for (int i = 0; i < list.size(); i++) {
      try {
        list.pick(i).accept(this);
        jsArrayPush(jsarray, currentObject);
      } catch (SException e) {
        ex = e;
        return;
      }
    }

    currentObject = jsarray;

  }


  public void visit(SNode node) {

    if (node instanceof SMap || node instanceof SObject) {
      visit((SMap) node);
      return;
    }

    if (node instanceof SList) {
      visit((SList) node);
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
    currentObject = text;
  }

  public Object build() throws SException {

    currentObject = null;
    ex = null;

    visit(root);

    if (ex != null)
      throw ex;

    return currentObject;

  }


}
