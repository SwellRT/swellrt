package org.swellrt.beta.model.java;

import java.util.HashSet;
import java.util.Set;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gson.JsonObject;

import jsinterop.annotations.JsIgnore;

public class SMapGson implements SMap {


  public static SMapGson create(JsonObject jso) {
    Preconditions.checkArgument(jso != null, "Null JsonObject argument");
		return new SMapGson(jso);
	}

  private final JsonObject jso;

  private SMapGson(JsonObject jso) {
		this.jso = jso;
	}

	@Override
	public SNode pick(String key) {

		if (key == null)
			return null;

    return SNodeGson.castToSNode(jso.get(key));
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
    return jso.has(key);
	}

	@Override
	public String[] keys() {
    Set<String> keyList = new HashSet<String>();

    jso.entrySet().forEach(e -> {
      keyList.add(e.getKey());
    });

    return keyList.toArray(new String[0]);
	}

	@Override
	public void clear() {
    throw new IllegalStateException("This node can't be mutated");
	}

	@Override
	public boolean isEmpty() {
    return size() == 0;
	}

	@Override
	public int size() {
    return jso.entrySet().size();
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
