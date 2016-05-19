package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableType;
import org.swellrt.model.ReadableTypeVisitable;
import org.swellrt.model.ReadableTypeVisitor;
import org.swellrt.model.adt.UnmutableBasicMap;
import org.swellrt.model.adt.UnmutableElementList;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.ValuesContainer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class UnmutableMap implements ReadableMap, ReadableTypeVisitable {

  /**
   * Adapt doc-based map elements to ReadableType's
   *
   */
  public static class ReadableMapElementAdapter implements
      UnmutableBasicMap.ElementAdapter<String, ReadableType> {

    private final Document document;
    private final UnmutableElementList<String, Void> values;
    private final UnmutableModel model;

    public ReadableMapElementAdapter(UnmutableModel model, Document document,
        UnmutableElementList<String, Void> values) {
      this.document = document;
      this.values = values;
      this.model = model;
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
          return UnmutableTypeFactory.deserialize(model, values, value);
        }

        @Override
        public ReadableType setValue(ReadableType value) {
          return null;
        }


      };

    }

  };

  public static UnmutableMap deserialize(UnmutableModel model, String substrateDocumentId) {

    final Document document = model.getDocument(substrateDocumentId);

    // Ignore on blips with no content
    if (document == null) return null;

    Doc.E eltMap = DocHelper.getElementWithTagName(document, MapType.TAG_MAP);



    Doc.E eltValues = DocHelper.getElementWithTagName(document, ValuesContainer.TAG_VALUES);
    @SuppressWarnings("unchecked")
    UnmutableElementList<String, Void> values =
        (UnmutableElementList<String, Void>) UnmutableElementList.create(
            new UnmutableElementList.ElementAdapter<String>() {

              @Override
              public String fromElement(E element) {
                return document.getAttribute(element, "v");
              }

            }, eltValues, document);


    @SuppressWarnings("unchecked")
    UnmutableBasicMap<String, ReadableType> map =
        (UnmutableBasicMap<String, ReadableType>) UnmutableBasicMap.create(
            new ReadableMapElementAdapter(model, document, values), eltMap, document);


    return new UnmutableMap(map, values);
  }

  private final UnmutableBasicMap<String, ReadableType> docBasedMap;
  private final UnmutableElementList<String, Void> values;


  private UnmutableMap(UnmutableBasicMap<String, ReadableType> map,
      UnmutableElementList<String, Void> values) {
    this.docBasedMap = map;
    this.values = values;
  }


  @Override
  public void accept(ReadableTypeVisitor visitor) {
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

  @Override
  public boolean hasKey(String key) {
    return docBasedMap.keySet().contains(key);
  }

  @Override
  public UnmutableMap asMap() {
    return this;
  }


  @Override
  public UnmutableString asString() {
    return null;
  }


  @Override
  public UnmutableList asList() {
    return null;
  }


  @Override
  public UnmutableText asText() {
    return null;
  }

  @Override
  public UnmutableFile asFile() {
    return null;
  }


  @Override
  public ReadableNumber asNumber() {
    return null;
  }


  @Override
  public ReadableBoolean asBoolean() {
    return null;
  }

}
