package org.swellrt.beta.model.json;

import org.swellrt.beta.client.wave.WaveDeps;

/**
 * A JSON object agnostic from underlying platform implementation
 * (Javascript/GWT or GSon)
 *
 * @author pablojan@gmail.com
 *
 */
public interface SJsonObject {

  public interface Factory {

    SJsonObject create();

    SJsonObject create(Object jso);

    SJsonObject parse(String json);

    String serialize(SJsonObject object);

  }

  public static SJsonObject cast(Object nativeJso) {
    return WaveDeps.sJsonFactory.create(nativeJso);
  }

  public static SJsonObject create() {
    return WaveDeps.sJsonFactory.create();
  }

  public static SJsonObject parse(String json) {
    return WaveDeps.sJsonFactory.parse(json);
  }

  public static String serialize(SJsonObject object) {
    return WaveDeps.sJsonFactory.serialize(object);
  }

  public SJsonObject addInt(String name, int value);

  public SJsonObject addLong(String name, long value);

  public SJsonObject addBoolean(String name, boolean value);

  public SJsonObject addString(String name, String value);

  public SJsonObject addDouble(String name, Double value);

  public SJsonObject addObject(String name, SJsonObject value);

  public Integer getInt(String name);

  public Long getLong(String name);

  public boolean getBoolean(String name);

  public String getString(String name);

  public Double getDouble(String name);

  public SJsonObject getObject(String name);

  public boolean has(String name);

  public Object getNative();

}
