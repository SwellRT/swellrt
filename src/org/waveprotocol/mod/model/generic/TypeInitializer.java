package org.waveprotocol.mod.model.generic;

public interface TypeInitializer {

  public static TypeInitializer ListTypeInitializer = new TypeInitializer() {

    @Override
    public String getType() {
      return ListType.TYPE;
    }

    @Override
    public String getSimpleValue() {
      return null;
    }
  };


  public static TypeInitializer MapTypeInitializer = new TypeInitializer() {

    @Override
    public String getType() {
      return MapType.TYPE;
    }

    @Override
    public String getSimpleValue() {
      return null;
    }
  };


  String getType();

  String getSimpleValue();

}
