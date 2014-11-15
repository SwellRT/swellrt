package org.waveprotocol.mod.model.generic;

public class TypeSerializer implements org.waveprotocol.wave.model.util.Serializer<Type> {

  protected Model model;

  protected TypeSerializer(Model model) {
    this.model = model;
  }

  @Override
  public String toString(Type x) {
    return x.serializeToModel();
  }

  @Override
  public Type fromString(String s) {

    if (s.startsWith(StringType.PREFIX)) {

      return StringType.fromString(model, s);

    } else if (s.startsWith(MapType.PREFIX)) {

      return MapType.fromString(model, s);

    } else if (s.startsWith(ListType.PREFIX)) {

      return ListType.fromString(model, s);
    }


    return null;
  }

  @Override
  public Type fromString(String s, Type defaultValue) {
    if (s == null) return defaultValue;
    Type instance = fromString(s);
    return instance != null ? instance : defaultValue;
  }

}
