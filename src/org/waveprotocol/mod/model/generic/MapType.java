package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapType implements ObservableBasicMap<String, GenericType> {

  private final static String ENTRY_TAG_NAME = "entry";
  private final static String KEY_ATTR_NAME = "k";
  private final static String VALUE_ATTR_NAME = "v";

  private final DocumentBasedBasicMap<Doc.E, String, GenericType> docBasedMap;
  private final Map<String, GenericType> map;

  public static MapType create(DocumentEventRouter<? super Doc.E, Doc.E, ?> router,
      Doc.E entryContainer) {

    DocumentBasedBasicMap<Doc.E, String, GenericType> docBasedMap =
        DocumentBasedBasicMap.create(router, entryContainer,
        Serializer.STRING, GenericType.serializer, ENTRY_TAG_NAME, KEY_ATTR_NAME, VALUE_ATTR_NAME);

    return new MapType(docBasedMap);
  }



  protected MapType(DocumentBasedBasicMap<Doc.E, String, GenericType> docBasedMap) {
    this.docBasedMap = docBasedMap;
    this.map = new HashMap<String, GenericType>();
  }

  @Override
  public GenericType get(String key) {

    if (!map.containsKey(key)) {
      map.put(key, docBasedMap.get(key));
    }

    return map.get(key);

  }

  @Override
  public boolean put(String key, GenericType value) {

    map.put(key, value);

    if (value.is(StringType.TYPE)) {
      docBasedMap.put(key, value);
    }

    return false;
  }

  @Override
  public Set<String> keySet() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void remove(String key) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addListener(
      org.waveprotocol.wave.model.adt.ObservableBasicMap.Listener<? super String, ? super GenericType> l) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeListener(
      org.waveprotocol.wave.model.adt.ObservableBasicMap.Listener<? super String, ? super GenericType> l) {
    // TODO Auto-generated method stub

  }

}
