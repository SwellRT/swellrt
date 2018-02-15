package org.swellrt.beta.client.wave;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.model.wave.SubstrateId;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

import com.google.common.base.Preconditions;

/**
 * Manages the creation of all documents in a particular wave.
 * This class is called from {@see WaveletDataImpl} if is properly
 * configured by its factory.
 * <p>
 * Based on original WaveDocuments class, adapted version for SwellRT
 *
 * @author pablojan@gmail.con (Pablo Ojanguren)
 *
 */
public final class SWaveDocuments<TextDocument extends DocumentOperationSink> implements DocumentFactory<DocumentOperationSink> {

  /**
   * Creates a wave's document collection.
   *
   * @param blipDocFactory factory for blip documents
   * @param dataDocFactory factory for data documents.
   */
  public static <B extends DocumentOperationSink> SWaveDocuments<B> create(
      DocumentFactory<B> textDocFactory, DocumentFactory<?> dataDocFactory) {
    return new SWaveDocuments<B>(textDocFactory, dataDocFactory);
  }

  private final DocumentFactory<TextDocument> textDocFactory;
  private final DocumentFactory<?> dataDocFactory;


  private final Map<SubstrateId, TextDocument> textDocRegistry = new HashMap<SubstrateId, TextDocument>();

  private SWaveDocuments(DocumentFactory<TextDocument> textDocFactory, DocumentFactory<?> dataDocFactory) {
    this.dataDocFactory = dataDocFactory;
    this.textDocFactory = textDocFactory;
  }

  @Override
  public DocumentOperationSink create(WaveletId waveletId, String docId,
      DocInitialization content) {

    if (SubstrateId.isText(docId)) {
      SubstrateId substrateId = SubstrateId.ofText(waveletId, docId);
      Preconditions.checkState(!textDocRegistry.containsKey(substrateId));

      TextDocument textDoc = textDocFactory.create(waveletId, docId, content);
      textDocRegistry.put(substrateId, textDoc);
      return textDoc;

    } else {
      return dataDocFactory.create(waveletId, docId, content);
    }

  }

  public TextDocument getTextDocument(SubstrateId substrateId) {
    return textDocRegistry.get(substrateId);
  }

  public TextDocument getTextDocument(String waveletId, String docId) {
    SubstrateId substrateId;
    try {
      substrateId = SubstrateId.ofText(ModernIdSerialiser.INSTANCE.deserialiseWaveletId(waveletId), docId);
    } catch (InvalidIdException e) {
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    }
    return getTextDocument(substrateId);
  }
}