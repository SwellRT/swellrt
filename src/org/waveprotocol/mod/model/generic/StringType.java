package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicValue;

public class StringType extends GenericType implements ObservableBasicValue<String> {

  public final static String TYPE = "string";
  public final static String SERIAL_PREFIX = "string";

  private String value;

  public static StringType deserialize(String s) {

    if (!s.startsWith(SERIAL_PREFIX)) return null;

    return new StringType(s.substring(SERIAL_PREFIX.length() + 1, s.length()));

  }

  private StringType(String value) {
    this.value = value;
  }

  @Override
  public boolean is(String type) {
    return type.equals(TYPE);
  }

  @Override
  public String serialize() {
    return SERIAL_PREFIX + value;
  }


  @Override
  public void set(String value) {
    this.value = value;
  }

  @Override
  public String get() {
    return value;
  }

  @Override
  public void addListener(
      org.waveprotocol.wave.model.adt.ObservableBasicValue.Listener<String> listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeListener(
      org.waveprotocol.wave.model.adt.ObservableBasicValue.Listener<String> listener) {
    // TODO Auto-generated method stub

  }


}
