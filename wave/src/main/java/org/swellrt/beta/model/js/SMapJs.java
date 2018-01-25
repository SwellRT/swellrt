package org.swellrt.beta.model.js;

import java.util.ArrayList;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.js.SNodeJs.Func;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsIgnore;

public class SMapJs implements SMap {



  public static SMapJs create(JavaScriptObject jso) {
    Preconditions.checkArgument(jso != null, "Null argument");
    Preconditions.checkArgument(SNodeJs.isObject(jso), "Not a JavaScriptObject");
		return new SMapJs(jso);
	}

	private final JavaScriptObject jso;
	private final JsoView jsv;

  private SMapJs(JavaScriptObject jso) {
		this.jso = jso;
		this.jsv = JsoView.as(jso);
	}

	@Override
	public SNode pick(String key) {

		if (key == null)
			return null;

    return SNodeJs.castToSNode(jsv.getObject(key));
	}

	@Override
	public SMap put(String key, SNode value) {
    throw new IllegalStateException("This node can't be mutated");
	}

	@Override
	public SMap put(String key, Object object) throws IllegalCastException {
    throw new IllegalStateException("This node can't be mutated");
	}

	@Override
	public void remove(String key) {
    throw new IllegalStateException("This node can't be mutated");
	}

  @Override
  public void removeSafe(String key) {
    throw new IllegalStateException("This node can't be mutated");
  }

	@Override
	public boolean has(String key) {
		return jsv.containsKey(key);
	}

	@Override
	public String[] keys() {

		ArrayList<String> keyList = new ArrayList<String>();

    SNodeJs.iterateObject(this.jso, new Func() {
			@Override
			public void apply(String key, Object value) {
				keyList.add(key);
			}
		});

		return keyList.toArray(new String[keyList.size()]);
	}

	@Override
	public void clear() {
    throw new IllegalStateException("This node can't be mutated");
	}

	@Override
	public boolean isEmpty() {
		return keys().length == 0;
	}

	@Override
	public int size() {
		return keys().length;
	}


  @SuppressWarnings("rawtypes")
  @JsIgnore
  @Override
  public void accept(SVisitor visitor) {
    visitor.visit(this);
  }

  //
  // -----------------------------------------------------
  //

  @Override
  public void set(String path, Object value) {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public void push(String path, Object value) {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public Object pop(String path) {
    return SNode.pop(this, path);
  }

  @Override
  public int length(String path) {
    return SNode.length(this, path);
  }

  @Override
  public boolean contains(String path, String property) {
    return SNode.contains(this, path, property);
  }

  @Override
  public void delete(String path) {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public Object get(String path) {
    return SNode.get(this, path);
  }

  @Override
  public SNode node(String path) throws SException {
    return SNode.node(this, path);
  }

  @Override
  public SMap asMap() {
    return this;
  }

  @Override
  public SList<? extends SNode> asList() {
    throw new IllegalStateException("Node is not a list");
  }

  @Override
  public String asString() {
    throw new IllegalStateException("Node is not a string");
  }

  @Override
  public double asDouble() {
    throw new IllegalStateException("Node is not a number");
  }

  @Override
  public int asInt() {
    throw new IllegalStateException("Node is not a number");
  }

  @Override
  public boolean asBoolean() {
    throw new IllegalStateException("Node is not a boolean");
  }

  @Override
  public SText asText() {
    throw new IllegalStateException("Node is not a text");
  }

  @Override
  public void addListener(SMutationHandler h, String path) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }

  @Override
  public void listen(SMutationHandler h) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }

  @Override
  public void removeListener(SMutationHandler h, String path) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }

  @Override
  public void unlisten(SMutationHandler h) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }
}
