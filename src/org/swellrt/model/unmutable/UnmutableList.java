package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableList;
import org.swellrt.model.ReadableType;
import org.swellrt.model.ReadableTypeVisitable;
import org.swellrt.model.ReadableTypeVisitor;
import org.swellrt.model.adt.UnmutableElementList;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.ValuesContainer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;

public class UnmutableList implements ReadableList<ReadableType>, ReadableTypeVisitable {

  public static class ReadableListElementAdapter implements
      UnmutableElementList.ElementAdapter<ReadableType> {

    private final Document document;
    private final UnmutableElementList<String, Void> values;
    private final UnmutableModel model;

    public ReadableListElementAdapter(UnmutableModel model, Document document,
        UnmutableElementList<String, Void> values) {
      this.model = model;
      this.document = document;
      this.values = values;
    }

    @Override
    public ReadableType fromElement(E element) {
      return UnmutableTypeFactory.deserialize(model, values, document.getAttribute(element, "r"));
    }

  };


  private final UnmutableElementList<ReadableType, Void> docBasedList;
  private final UnmutableElementList<String, Void> values;


  public static UnmutableList deserialize(UnmutableModel model, String substrateDocumentId) {

    final Document document = model.getDocument(substrateDocumentId);
    Doc.E eltlist = DocHelper.getElementWithTagName(document, ListType.TAG_LIST);



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
    UnmutableElementList<ReadableType, Void> list =
        (UnmutableElementList<ReadableType, Void>) UnmutableElementList.create(
            new ReadableListElementAdapter(model, document, values), eltlist,
            document);


    return new UnmutableList(list, values);
  }


  private UnmutableList(UnmutableElementList<ReadableType, Void> list,
      UnmutableElementList<String, Void> values) {
    this.docBasedList = list;
    this.values = values;

  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public ReadableType get(int index) {
    return docBasedList.get(index);
  }

  @Override
  public int size() {
    return docBasedList.size();
  }

  @Override
  public Iterable<ReadableType> getValues() {
    return docBasedList.getValues();
  }


  @Override
  public UnmutableMap asMap() {
    return null;
  }


  @Override
  public UnmutableString asString() {
    return null;
  }


  @Override
  public UnmutableList asList() {
    return this;
  }


  @Override
  public UnmutableText asText() {
    return null;
  }

}
