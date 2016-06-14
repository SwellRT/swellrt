package org.swellrt.model.generic;

public class MapSerializer implements org.waveprotocol.wave.model.util.Serializer<Type> {

  private final MapType parent;

  protected MapSerializer(MapType parent) {
    this.parent = parent;
  }

  @Override
  public String toString(Type v) {
    return v.serialize();
  }

  @Override
  public Type fromString(String ref) {
    return Type.deserialize(parent, ref);
  }

  @Override
  public Type fromString(String s, Type defaultValue) {
    if (s == null) return defaultValue;
    Type instance = fromString(s);
    return instance != null ? instance : defaultValue;
  }

}
