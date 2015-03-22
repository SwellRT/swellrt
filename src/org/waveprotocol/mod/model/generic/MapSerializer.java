package org.waveprotocol.mod.model.generic;

public class MapSerializer implements org.waveprotocol.wave.model.util.Serializer<Type> {

  protected Model model;

  protected MapSerializer(Model model) {
    this.model = model;
  }

  @Override
  public String toString(Type x) {
    return x.serializeToModel();
  }

  @Override
  public Type fromString(String s) {

    if (s.startsWith(StringType.PREFIX)) {

      return StringType.createAndAttach(model, s);

    } else if (s.startsWith(MapType.PREFIX)) {

      return MapType.createAndAttach(model, s);

    } else if (s.startsWith(ListType.PREFIX)) {

      return ListType.createAndAttach(model, s);
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
