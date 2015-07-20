package org.swellrt.model.adt;

import org.waveprotocol.wave.model.adt.BasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class UnmutableBasicMap<K, V> implements BasicMap<K, V> {

  public interface ElementAdapter<K, V> {

    Entry<K, V> fromElement(Doc.E element);

  }

  private final Document document;
  private final Doc.E parent;
  private final ElementAdapter<K, V> adapter;
  private Map<K, V> values;


  @SuppressWarnings("rawtypes")
  public static UnmutableBasicMap<?, ?> create(ElementAdapter<?, ?> adapter, Doc.E parent,
      Document document) {
    @SuppressWarnings("unchecked")
    UnmutableBasicMap<?, ?> map = new UnmutableBasicMap(adapter, parent, document);
    map.load();
    return map;

  }

  private UnmutableBasicMap(ElementAdapter<K, V> adapter, Doc.E parent, Document document) {
    this.document = document;
    this.parent = parent;
    this.adapter = adapter;
    this.values = new HashMap<K, V>();
  }


  private void load() {
    Doc.E entry = DocHelper.getFirstChildElement(document, parent);
    while (entry != null) {
      Entry<K, V> mapEntry = adapter.fromElement(entry);
      values.put(mapEntry.getKey(), mapEntry.getValue());
      entry = DocHelper.getNextSiblingElement(document, entry);
    }
  }

  @Override
  public V get(K key) {
    return values.get(key);
  }

  @Override
  public boolean put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    return values.keySet();
  }

  @Override
  public void remove(K key) {
    throw new UnsupportedOperationException();
  }

}
