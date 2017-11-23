package org.swellrt.beta.client.serverless;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.js.editor.STextRemoteWeb;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.playback.DocOpContextCache;
import org.waveprotocol.wave.client.wave.LazyContentDocument;
import org.waveprotocol.wave.client.wave.SimpleDiffDoc;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Service")
public class ServiceServerless {



  private Map<WaveId, SWaveObject> objects = new HashMap<WaveId, SWaveObject>();

  //
  // Fake stuff for the serverless version of swell client.
  //

  private static ParticipantId participant = ParticipantId.ofUnsafe("fake@local.net");

  private static IdGenerator idGenerator = new IdGeneratorImpl("local.net",
      new IdGeneratorImpl.Seed() {
        @Override
        public String get() {
          return "ABCDEFGHIK"; // the seed :D
        }
      });

  private static SWaveNodeManager.NodeFactory nodeFactory = new SWaveNodeManager.NodeFactory() {

    @Override
    public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
        Blip blip) {

      try {
        return new STextRemoteWeb(nodeManager, substrateId, blip,
            LazyContentDocument.create(Editor.ROOT_REGISTRIES,
                SimpleDiffDoc.create(DocOpUtil.docInitializationFromXml(""), null),
                DocOpContextCache.VOID));

      } catch (XmlParseException e) {
        throw new RuntimeException(e);
      }

    }

  };

  @JsIgnore
  protected static ServiceServerless create() {
    return new ServiceServerless();
  }


  /**
   * Open or create a Swell object. <br>
   * <p>
   * TODO implement this method with same syntax as {@code OpenOperation}
   *
   * @param id
   *          (optional) object id.
   * @return a {@code SWaveObject} instance.
   */
  public SObject open(@JsOptional String id) {


    WaveId waveId = null;

    FakeWaveView wave;

    try {
      waveId = WaveId.of("local.net", id);
    } catch (IllegalArgumentException e) {
    }

    if (waveId == null)
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant).build();
    else if (!objects.containsKey(waveId))
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant).with(waveId)
          .build();
    else
      return objects.get(waveId);

    SWaveNodeManager nodeManager = SWaveNodeManager.of(participant,
        idGenerator, "local.net", wave, null, nodeFactory);
    SWaveObject object = SWaveObject.materialize(nodeManager);

    objects.put(waveId, object);

    return object;

  }

}
