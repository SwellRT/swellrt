package org.swellrt.beta.model.js;

import org.swellrt.beta.common.PathWalker;
import org.swellrt.beta.common.SException;
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
  PathWalker path;

  SException ex;

  @Override
  public SNode getNode(String path, SNode root) throws SException {

    if (path == null || path.isEmpty())
      return root;

    this.node = root;
    this.path = new PathWalker(path);

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


    if (primitive.getType() == SPrimitive.TYPE_JSO) {

      /* Traverse the JavaScriptObject if there is still path elements */
      JavaScriptObject jso = (JavaScriptObject) getNode(path.get(),
          primitive.getValue());
      node = new SPrimitive(jso, new SNodeAccessControl(), primitive, path.getConsumed(),
          path.get());

    } else {
      node = primitive;
    }
  }

  @Override
  public void visit(SMap map) {

    String pathElement = path.nextPathElement();
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

    String pathElement = path.nextPathElement();
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

    PathWalker pathw = new PathWalker(path);

    String pathElement = pathw.nextPathElement();
    while (pathElement != null && !pathElement.isEmpty() && jso != null) {
      jso = JsoView.as(jso).getJso(pathElement);
      pathElement = pathw.nextPathElement();
    }

    return jso;

  }

  @Override
  public SViewBuilder jsonBuilder(SNode node) {

    // Check more specific type first
    if (node instanceof SWaveNode) {
      return new JsViewVisitor<SWaveNode>((SWaveNode) node);
    } else {
      return new JsViewVisitor<SNode>(node);
    }

  }


}
