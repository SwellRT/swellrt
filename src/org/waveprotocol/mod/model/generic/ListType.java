package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.adt.ObservableElementList;

public class ListType extends GenericType implements
    ObservableElementList<GenericType, GenericType.Initializer> {


  public final static String TYPE = "list";
  public final static String SERIAL_PREFIX = "list";


  @Override
  public boolean is(String type) {
    return type.equals(TYPE);
  }

  @Override
  public String serialize() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterable<GenericType> getValues() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean remove(GenericType element) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public GenericType add(Initializer initialState) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public GenericType add(int index, Initializer initialState) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int indexOf(GenericType element) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public GenericType get(int index) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void clear() {
    // TODO Auto-generated method stub

  }

  @Override
  public void addListener(
      org.waveprotocol.wave.model.adt.ObservableElementList.Listener<GenericType> listener) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeListener(
      org.waveprotocol.wave.model.adt.ObservableElementList.Listener<GenericType> listener) {
    // TODO Auto-generated method stub

  }

}
