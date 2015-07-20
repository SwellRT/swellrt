package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableList;
import org.swellrt.model.ReadableType;
import org.swellrt.model.ReadableTypeFactory;
import org.swellrt.model.TypeVisitor;
import org.swellrt.model.adt.UnmutableElementList;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;

public class UnmutableList implements ReadableList {

  public class ReadableListElementAdapter implements
      UnmutableElementList.ElementAdapter<ReadableType> {

    @Override
    public ReadableType fromElement(E element) {
      return typeFactory.get(document.getAttribute(element, "r"));
    }

  };


  private UnmutableElementList<ReadableType, Void> docBasedList;
  private final Document document;
  private final Doc.E parent;
  private final ReadableTypeFactory typeFactory;


  protected static UnmutableList create(ReadableTypeFactory typeFactory, Document document,
      Doc.E parent) {
    UnmutableList list = new UnmutableList(typeFactory, document, parent);
    list.load();
    return list;
  }


  private UnmutableList(ReadableTypeFactory typeFactory, Document document, Doc.E parent) {
    this.typeFactory = typeFactory;
    this.document = document;
    this.parent = parent;
  }

  @SuppressWarnings("unchecked")
  private void load() {
    this.docBasedList =
        (UnmutableElementList<ReadableType, Void>) UnmutableElementList.create(
            new ReadableListElementAdapter(), parent,
            document);
  }

  @Override
  public void accept(TypeVisitor visitor) {
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

}
