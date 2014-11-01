package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.util.Serializer;

public abstract class GenericType {

  public interface Initializer {

  }


  public static final Serializer<GenericType> serializer =  new Serializer<GenericType>() {

    @Override
    public String toString(GenericType x) {
      return x.serialize();
    }

    @Override
    public GenericType fromString(String s) {


      return null;
    }

    @Override
    public GenericType fromString(String s, GenericType defaultValue) {
      // TODO Auto-generated method stub
      return null;
    }

  };

  public abstract boolean is(String type);

  public abstract String serialize();


}
