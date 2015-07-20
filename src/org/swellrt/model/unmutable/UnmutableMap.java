package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableType;
import org.swellrt.model.ReadableTypeFactory;
import org.swellrt.model.TypeVisitor;
import org.swellrt.model.adt.UnmutableBasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class UnmutableMap implements ReadableMap {

  /**
   * Adapt doc-based map elements to ReadableType's
   * 
   */
  public class ReadableMapElementAdapter implements
      UnmutableBasicMap.ElementAdapter<String, ReadableType> {

    public ReadableMapElementAdapter() {
    }

    @Override
    public Entry<String, ReadableType> fromElement(E element) {

      final String key = document.getAttribute(element, "k");
      final String value = document.getAttribute(element, "v");

      return new Map.Entry<String, ReadableType>() {

        @Override
        public String getKey() {
          return key;
        }

        @Override
        public ReadableType getValue() {
          return typeFactory.get(value);
        }

        @Override
        public ReadableType setValue(ReadableType value) {
          return null;
        }


      };

    }

  };

  private UnmutableBasicMap<String, ReadableType> docBasedMap;
  private final Document document;
  private final Doc.E parent;
  private final ReadableTypeFactory typeFactory;

  protected static UnmutableMap create(ReadableTypeFactory typeFactory, Document document,
      Doc.E parent) {
    UnmutableMap map = new UnmutableMap(typeFactory, document, parent);
    map.load();
    return map;
  }

  private UnmutableMap(ReadableTypeFactory typeFactory, Document document, Doc.E parent) {
    this.typeFactory = typeFactory;
    this.document = document;
    this.parent = parent;
  }

  @SuppressWarnings("unchecked")
  private void load() {
    this.docBasedMap =
        (UnmutableBasicMap<String, ReadableType>) UnmutableBasicMap.create(
            new ReadableMapElementAdapter(), parent, document);
  }

  @Override
  public void accept(TypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public ReadableType get(String key) {
    return docBasedMap.get(key);
  }

  @Override
  public Set<String> keySet() {
    return docBasedMap.keySet();
  }

}
