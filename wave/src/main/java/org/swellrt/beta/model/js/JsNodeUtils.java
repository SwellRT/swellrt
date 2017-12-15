package org.swellrt.beta.model.js;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.ModelFactory;
import org.swellrt.beta.model.PathNavigator;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SNodeUtils;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SViewBuilder;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.wave.mutable.SWaveNode;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

public class JsNodeUtils implements SNodeUtils, SVisitor<SNode> {

  SNode node;
  PathNavigator path;
  SException ex;

  @Override
  public SNode getNode(String path, SNode root) throws SException {

    if (path == null || path.isEmpty())
      return root;

    this.ex = null;
    this.node = root;
    this.path = new PathNavigator(path);

    visit(root);

    if (ex != null)
      throw ex;

    return node;
  }



  @SuppressWarnings({ "unchecked", "rawtypes" })
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

  }


  @Override
  public void visit(SPrimitive primitive) {


    if (primitive.isJso() && !path.currentPath().isEmpty()) {

      // Traverse the Json object if there is still path elements
      // return value could be a primitive type or a Json object
      Object value = ModelFactory.instance.traverseJsonObject(primitive.getValue(), path.currentPath());

      if (value != null) {
        node = new SPrimitive(value, new SNodeAccessControl(), primitive, path.consumedPath(),
            path.currentPath());
      } else {
        node = null;
      }

    } else {
      node = primitive;
    }
  }

  @Override
  public void visit(SMap map) {

    String pathElement = path.next();
    if (pathElement != null) {

      try {
        node = map.pick(pathElement);
        visit(node);
      } catch (SException e) {
        ex = e;
        return;
      }

    }


  }

  @Override
  public void visit(SList<SNode> list) {

    String pathElement = path.next();
    if (pathElement != null) {

      try {

        int index = Integer.valueOf(pathElement);
        node = list.pick(index);
        visit(node);
      } catch (SException e) {
        ex = e;
        return;
      } catch (NumberFormatException e) {
        ex = new SException(SException.OPERATION_EXCEPTION, e, "Path element is not an index");
        return;
      }

    }

  }

  @Override
  public void visit(SText text) {
    node = text;
  }

  //
  // Non visitor methods
  //

  @Override
  public Object getNode(String path, Object root) {

    if (!(root instanceof JavaScriptObject))
      return null;

    JavaScriptObject jso = (JavaScriptObject) root;

    if (path == null || path.isEmpty())
      return jso;

    PathNavigator pathw = new PathNavigator(path);

    String pathElement = pathw.next();
    while (pathElement != null && !pathElement.isEmpty() && jso != null) {
      jso = JsoView.as(jso).getJso(pathElement);
      pathElement = pathw.next();
    }

    return jso;

  }

  @Override
  public SViewBuilder jsonBuilder(SNode node) {

    // Check more specific type first
    if (node instanceof SWaveNode) {
      return new SViewBuilderJs<SWaveNode>((SWaveNode) node);
    } else {
      return new SViewBuilderJs<SNode>(node);
    }

  }

  private native boolean isUndefinedOrNull(Object value) /*-{
    return value === undefined || value === null;
  }-*/;

  @Override
  public Integer castToInteger(Object value) {

    if (isUndefinedOrNull(value))
      return null;

    try {
      return (int) value;
    } catch (RuntimeException e) {
      return null;
    }

  }


}
