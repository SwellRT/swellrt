package org.waveprotocol.wave.model.extended;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocHelper.NodeAction;

import java.util.ArrayList;
import java.util.List;

public class ExtendedDocHelper {


  static class ExtractByTagName<N> implements NodeAction<N> {

    private final List<Doc.E> elementList;
    private final String tagName;
    private final Document doc;

    public ExtractByTagName(Document doc, String tagName) {
      this.elementList = new ArrayList<Doc.E>();
      this.tagName = tagName;
      this.doc = doc;
    }

    @Override
    public void apply(N node) {
      Doc.E element = doc.asElement((Doc.N) node);
      if (element != null && doc.getTagName(element).equalsIgnoreCase(tagName)) {
        elementList.add(element);
      }
    }


    public List<Doc.E> getElementList() {
      return elementList;
    }

  }

  public static <N> List<E> getAllElementsByTagName(String tagName,
      Document doc) {

    ExtractByTagName<Doc.N> byTagExtractor =
        new ExtractByTagName<Doc.N>(doc, tagName);

    DocHelper.traverse(doc, doc.getFirstChild(doc.getDocumentElement()), byTagExtractor);

    return byTagExtractor.getElementList();
  }

}
