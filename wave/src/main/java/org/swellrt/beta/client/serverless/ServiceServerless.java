package org.swellrt.beta.client.serverless;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.js.editor.STextRemoteWeb;
import org.swellrt.beta.client.operation.Operation.Callback;
import org.swellrt.beta.client.operation.impl.OpenOperation;
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

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Service")
public class ServiceServerless {

  private Map<String, SWaveObject> objects = new HashMap<String, SWaveObject>();


  private static IdGenerator idGenerator = new IdGeneratorImpl("local.net",
      new IdGeneratorImpl.Seed() {
        @Override
        public String get() {
  return"ABCDEFGHIK"; // the seed :D
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

  public static ServiceServerless create() {

    return new ServiceServerless();

  }


  public void open(OpenOperation.Options options, Callback<OpenOperation.Response> callback) {

    String id = "";

    if (options.getPrefix() != null)
      id += options.getPrefix();

    if (options.getId() != null)
      id += "+" + options.getId();

    FakeWaveView wave;

    try {
      WaveId waveId = WaveId.of("local.net", id);
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(waveId).build();
    } catch (IllegalArgumentException e) {
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).build();
    }

    SWaveNodeManager nodeManager = SWaveNodeManager.of(ParticipantId.ofUnsafe("none@local.net"),
        idGenerator, "local.net", wave, null, nodeFactory);
    SWaveObject object = SWaveObject.materialize(nodeManager);

    objects.put(object.getId(), object);

    callback.onSuccess(object);

  }

}
