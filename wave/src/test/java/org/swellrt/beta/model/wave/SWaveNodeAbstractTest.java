package org.swellrt.beta.model.wave;

import java.util.Random;

import org.swellrt.beta.client.wave.DummyLazyContentFactory;
import org.swellrt.beta.client.wave.SWaveDocuments;
import org.swellrt.beta.model.ModelFactory;
import org.swellrt.beta.model.java.JavaModelFactory;
import org.swellrt.beta.model.presence.SSession;
import org.swellrt.beta.model.presence.SSessionManager;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.wave.LazyContentDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;

import junit.framework.TestCase;

/**
 * Base class for tests regarding SNodeRemote hierarchy
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public abstract class SWaveNodeAbstractTest extends TestCase {

  private static ModelFactory modelFactory = new JavaModelFactory();

  protected IdGenerator idGenerator;
  protected FakeWaveView wave;
  protected SSession session;
  protected SSessionManager sessionProvider;
  protected SWaveObject object;
  protected SWaveDocuments<LazyContentDocument> docRegistry;
  protected ParticipantId participant = ParticipantId.ofUnsafe("tom@acme.com");


  protected void setUp() throws Exception {

    docRegistry = SWaveDocuments.create(new DummyLazyContentFactory(Editor.ROOT_REGISTRIES),

        ObservablePluggableMutableDocument.createFactory(new SchemaProvider() {

          @Override
          public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
            return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
          }
        }));

    idGenerator = new IdGeneratorImpl("acme.com", new Seed() {

      Random r = new Random(System.currentTimeMillis());

      @Override
      public String get() {
        return r.nextInt(1000) + "";
      }

    });

    wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(docRegistry)
        .with(participant).build();
    session = new SSession("fake-session-id", participant,
        RgbColor.WHITE, "Fake Name", "fakie");
    sessionProvider = new SSessionManager(session);



    SWaveNodeManager nodeManager = SWaveNodeManager.create(sessionProvider,
        idGenerator, "example.com", wave, null, docRegistry);
    object = nodeManager.getSWaveObject();

    // A different way to create a fake wave
    /*
    SchemaProvider SCHEMA_PROVIDER = new SchemaProvider() {

      @Override
      public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
        if (Model.isModelWaveletId(waveletId)) {
          if (TextType.isTextBlipId(documentId)) {
            return ModelSchemas.TEXT_DOCUMENT_SCHEMA;
          }
          // TODO Add more schemas for List, Root, Map...
        }
        return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
      }
    };

    IdGenerator ID_GENERATOR = FakeIdGenerator.create();

    String DOMAIN = "example.com";

    FakeWaveView wave = FakeWaveView.builder(SCHEMA_PROVIDER).with(ID_GENERATOR).build();


    MutableCObject object = MutableCObject.ofWave(DOMAIN, wave);
    */
  }


}
