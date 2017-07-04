package org.swellrt.beta.model.js;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.common.PathWalker;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.PathNodeExtractor;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;

import com.google.gwt.core.client.JavaScriptObject;

public class JsPathNodeExtractor implements PathNodeExtractor, SVisitor<SNode> {

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
      JavaScriptObject jso = (JavaScriptObject) PlatformBasedFactory
          .extractNode((JavaScriptObject) primitive.value(), path.get());
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
        node = map.node(pathElement);
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
        node = list.node(index);
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

}
