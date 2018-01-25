package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.model.json.SJsonObject;
import org.waveprotocol.wave.client.common.util.JsoView;

public class SJsonObjectWeb implements SJsonObject {

  private final JsoView jso;

  public SJsonObjectWeb() {
    this.jso = JsoView.create();
  }

  protected SJsonObjectWeb(JsoView jso) {
    this.jso = jso;
  }

  @Override
  public SJsonObject addInt(String name, int value) {
    jso.setNumber(name, value);
    return this;
  }

  @Override
  public SJsonObject addLong(String name, long value) {
    jso.setNumber(name, value);
    return this;
  }

  @Override
  public SJsonObject addBoolean(String name, boolean value) {
    jso.setBoolean(name, value);
    return this;
  }

  @Override
  public SJsonObject addString(String name, String value) {
    jso.setString(name, value);
    return this;
  }

  @Override
  public SJsonObject addDouble(String name, Double value) {
    jso.setNumber(name, value);
    return this;
  }

  @Override
  public SJsonObject addObject(String name, SJsonObject value) {
    if (value instanceof SJsonObjectWeb) {
      SJsonObjectWeb jow = (SJsonObjectWeb) value;
      jso.setJso(name, jow.jso);
    }
    return this;
  }

  @Override
  public int getInt(String name) {
    return (int) jso.getNumber(name);
  }

  @Override
  public long getLong(String name) {
    return (long) jso.getNumber(name);
  }

  @Override
  public boolean getBoolean(String name) {
    return jso.getBoolean(name);
  }

  @Override
  public String getString(String name) {
    return jso.getString(name);
  }

  @Override
  public SJsonObject getObject(String name) {
    return new SJsonObjectWeb(jso.getJsoView(name));
  }

  @Override
  public boolean has(String name) {
    return jso.containsKey(name);
  }

  protected JsoView getJsoView() {
    return jso;
  }

  @Override
  public Object getNative() {
    return jso;
  }

}
