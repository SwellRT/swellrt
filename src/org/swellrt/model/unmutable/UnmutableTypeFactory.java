package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableType;
import org.swellrt.model.ReadableTypeFactory;
import org.swellrt.model.adt.UnmutableElementList;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

public class UnmutableTypeFactory implements ReadableTypeFactory {


  private final ReadableWaveletData waveletData;
  private final UnmutableElementList<String, Void> stringData;

  protected UnmutableTypeFactory(ReadableWaveletData waveletData,
      UnmutableElementList<String, Void> stringData) {
    this.waveletData = waveletData;
    this.stringData = stringData;
  }

  @Override
  public ReadableType get(String documentId) {

    String tag = null;
    String type = null;


    if (documentId.startsWith(StringType.PREFIX)) {
      tag = null;
      type = StringType.PREFIX;

    } else if (documentId.startsWith(MapType.PREFIX)) {
      tag = MapType.ROOT_TAG;
      type = MapType.PREFIX;

    } else if (documentId.startsWith(ListType.PREFIX)) {
      tag = ListType.ROOT_TAG;
      type = ListType.PREFIX;

    } else if (documentId.startsWith(TextType.PREFIX)) {
      tag = null;
      type = TextType.PREFIX;
    }


    return get(documentId, tag, type);
  }


  @Override
  public ReadableType get(String documentId, String tag, String type) {

    if (type.equals(StringType.PREFIX)) {

      int index = Integer.parseInt(documentId.split("\\+")[1]);
      return new UnmutableString(stringData.get(index));

    } else if (type.equals(ListType.PREFIX)) {

      Document document = waveletData.getDocument(documentId).getContent().getMutableDocument();
      Doc.E parent = DocHelper.getElementWithTagName(document, tag);
      return UnmutableList.create(this, document, parent);

    } else if (type.equals(MapType.PREFIX)) {

      Document document = waveletData.getDocument(documentId).getContent().getMutableDocument();
      Doc.E parent = DocHelper.getElementWithTagName(document, tag);
      return UnmutableMap.create(this, document, parent);

    } else if (type.equals(TextType.PREFIX)) {
      return new UnmutableText(waveletData.getDocument(documentId));
    }

    return null;

  }

}
