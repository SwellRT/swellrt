package org.swellrt.model.adt;

import org.waveprotocol.wave.model.adt.ElementList;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;

import java.util.ArrayList;
import java.util.List;


public class UnmutableElementList<T, I> implements ElementList<T, I> {

  public interface ElementAdapter<T> {

    T fromElement(Doc.E element);

  }

  private final Document document;
  private final Doc.E parent;
  private final ElementAdapter<T> adapter;
  private List<T> values;

  @SuppressWarnings("rawtypes")
  public static UnmutableElementList<?, ?> create(ElementAdapter<?> adapter,
      Doc.E parent, Document document) {
    @SuppressWarnings("unchecked")
    UnmutableElementList<?, ?> list = new UnmutableElementList(adapter, parent, document);
    list.load();
    return list;

  }

  private UnmutableElementList(ElementAdapter<T> adapter, Doc.E parent, Document document) {
    this.parent = parent;
    this.adapter = adapter;
    this.values = new ArrayList<T>();
    this.document = document;
  }


  private void load() {
    Doc.E entry = DocHelper.getFirstChildElement(document, parent);
    while (entry != null) {
      values.add(adapter.fromElement(entry));
      entry = DocHelper.getNextSiblingElement(document, entry);
    }
  }

  @Override
  public Iterable<T> getValues() {
    return values;
  }

  @Override
  public boolean remove(Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T add(Object initialState) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T add(int index, Object initialState) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(T element) {
    return values.indexOf(element);
  }

  @Override
  public T get(int index) {
    return values.get(index);
  }

  @Override
  public int size() {
    return values.size();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

}
