package org.swellrt.beta.model.java;

import java.util.ArrayList;
import java.util.List;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gson.JsonArray;

public class SListGson implements SList<SNode> {

  public static SListGson create(JsonArray jso) {
    Preconditions.checkArgument(jso != null, "Null JsonArray argument");
    return new SListGson(jso);
  }

  private final JsonArray jso;

  protected SListGson(JsonArray jso) {
    super();
    this.jso = jso;
  }

  @Override
  public SNode node(String path) throws SException {
    return SNode.node(this, path);
  }

  @Override
  public void set(String path, Object value) {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public Object get(String path) {
    return SNode.get(this, path);
  }

  @Override
  public void push(String path, Object value) {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public Object pop(String path) {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public void delete(String path) {
    throw new IllegalStateException("This node can't be mutated");
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
  public void accept(SVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public SNode pick(int index) throws SException {
    return SNodeGson.castToSNode(jso.get(index));
  }

  @Override
  public SList<SNode> addAt(Object object, int index) throws SException {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public SList<SNode> add(Object object) throws SException {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public SList<SNode> remove(int index) throws SException {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public int indexOf(SNode node) throws SException {
    return getList().indexOf(node);
  }

  @Override
  public void clear() throws SException {
    throw new IllegalStateException("This node can't be mutated");
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public int size() {
    return jso.size();
  }

  protected List<SNode> getList() {
    List<SNode> values = new ArrayList<SNode>();
    jso.forEach(e -> {
      values.add(SNodeGson.castToSNode(e));
    });
    return values;
  }

  @Override
  public Iterable<SNode> values() {
    return getList();
  }

  //
  //
  //

  @Override
  public SMap asMap() {
    throw new IllegalStateException("Node is not a map");
  }

  @Override
  public SList<? extends SNode> asList() {
    return this;
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
