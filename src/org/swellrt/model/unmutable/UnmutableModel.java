package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableModel;
import org.swellrt.model.ReadableTypeFactory;
import org.swellrt.model.adt.UnmutableElementList;
import org.swellrt.model.generic.MapType;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Set;

/**
 * An unmutable SwellRT data model that parse all Document's into Java objects
 * at construction time.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public class UnmutableModel implements ReadableModel {

  private static final Log LOG = Log.get(UnmutableModel.class);

  private final static String ROOT_DOC = "model+root";

  private final ReadableWaveletData waveletData;

  private final Document document;
  private UnmutableElementList<String, Void> strings;

  private ReadableTypeFactory typeFactory = null;

  private ReadableMap root = null;


  public static UnmutableModel create(ReadableWaveletData waveletData) {
    // Avoid trouble with old swellrt wavelets
    if (waveletData.getDocument(ROOT_DOC) == null
        || waveletData.getDocument(ROOT_DOC).getContent() == null
        || waveletData.getDocument(ROOT_DOC).getContent().getMutableDocument() == null)
      return null;

    UnmutableModel model = new UnmutableModel(waveletData);
    model.load();

    return model;
  }

  private UnmutableModel(ReadableWaveletData waveletData) {

    // Get wavelet and root document
    this.waveletData = waveletData;
    this.document = waveletData.getDocument(ROOT_DOC).getContent().getMutableDocument();

  }

  @SuppressWarnings("unchecked")
  private void load() {

    // Get the (doc-based) string list
    Doc.E stringsElement = DocHelper.getElementWithTagName(this.document, "strings");
    this.strings =
        (UnmutableElementList<String, Void>) UnmutableElementList.create(
            new UnmutableElementList.ElementAdapter<String>() {

              @Override
              public String fromElement(E element) {
                return document.getAttribute(element, "v");
              }

            }, stringsElement, this.document);
  }

  public UnmutableElementList<String, Void> strings() {
    return strings;
  }

  @Override
  public String getWaveId() {
    return ModernIdSerialiser.INSTANCE.serialiseWaveId(waveletData.getWaveId());
  }

  @Override
  public String getWaveletId() {
    return ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletData.getWaveletId());
  }

  @Override
  public Set<ParticipantId> getParticipants() {
    return waveletData.getParticipants();
  }

  @Override
  public ReadableMap getRoot() {

    if (root == null)
      root = (ReadableMap) getTypeFactory().get(ROOT_DOC, MapType.ROOT_TAG, MapType.PREFIX);

    return root;
  }

  @Override
  public ReadableTypeFactory getTypeFactory() {

    if (typeFactory == null) {
      typeFactory = new UnmutableTypeFactory(waveletData, strings);
    }

    return typeFactory;
  }


}
