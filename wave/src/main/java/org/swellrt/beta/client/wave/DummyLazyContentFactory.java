package org.swellrt.beta.client.wave;

import java.util.Optional;

import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.wave.DocOpContext;
import org.waveprotocol.wave.client.wave.DocOpTracker;
import org.waveprotocol.wave.client.wave.LazyContentDocument;
import org.waveprotocol.wave.client.wave.SimpleDiffDoc;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;


public class DummyLazyContentFactory implements DocumentFactory<LazyContentDocument> {

  Registries registries;

  public DummyLazyContentFactory(Registries registries) {
    this.registries = registries;
  }

  @Override
  public LazyContentDocument create(WaveletId waveletId, String docId, DocInitialization content) {

    SimpleDiffDoc noDiff = SimpleDiffDoc.create(content, null);

    return LazyContentDocument.create(registries, noDiff,

        // Adapt the global DocOp cache to this particular blip
        new DocOpTracker() {

          @Override
          public void add(DocOp op, DocOpContext opCtx) {

          }

          @Override
          public Optional<DocOpContext> fetch(DocOp op) {
            return Optional.empty();
          }

        },

        null);

  }

}
