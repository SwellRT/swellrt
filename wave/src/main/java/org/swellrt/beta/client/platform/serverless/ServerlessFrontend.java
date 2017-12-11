package org.swellrt.beta.client.platform.serverless;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.platform.web.editor.STextRemoteWeb;
import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.client.rest.operations.params.CredentialData;
import org.swellrt.beta.client.rest.operations.params.ObjectId;
import org.swellrt.beta.client.rest.operations.params.ObjectName;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.client.wave.DocOpTracker;
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

@JsType(namespace = "swell", name = "Serverless")
public class ServerlessFrontend implements ServiceFrontend {

  private static final String WAVE_DOMAIN = "local.net";

  private ParticipantId participant = ParticipantId.ofUnsafe("fake@local.net");

  private IdGenerator idGenerator = new IdGeneratorImpl("local.net", new IdGeneratorImpl.Seed() {
    @Override
    public String get() {
      return "ABCDEFGHIK"; // the seed :D
    }
  });

  private SWaveNodeManager.NodeFactory nodeFactory = new SWaveNodeManager.NodeFactory() {

    @Override
    public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
        Blip blip) {

      try {
        return new STextRemoteWeb(nodeManager, substrateId, blip,
            LazyContentDocument.create(Editor.ROOT_REGISTRIES,
                SimpleDiffDoc.create(DocOpUtil.docInitializationFromXml(""), null),
                DocOpTracker.VOID, DiffProvider.VOID_DOC_DIFF_PROVIDER));

      } catch (XmlParseException e) {
        throw new RuntimeException(e);
      }

    }

  };

  private Map<WaveId, SWaveObject> objects = new HashMap<WaveId, SWaveObject>();

  @Override
  public ProfileManager getProfilesManager() {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void addConnectionHandler(ConnectionHandler h) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void removeConnectionHandler(ConnectionHandler h) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void createUser(Account options, Callback<Account> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void login(Credential options,
      Callback<Account> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void logout(Credential options,
      Callback<Void> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void resume(Credential options,
      Callback<Account> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void open(ObjectId options,
      Callback<SObject> callback) {

    try {

      SObject o = openSync(options.getId());
      callback.onSuccess(o);

    } catch (Exception e) {
      callback.onError(new SException(SException.OPERATION_EXCEPTION, e));
    }

  }

  @Override
  public void close(ObjectId options,
      Callback<Void> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void query(org.swellrt.beta.client.rest.operations.QueryOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.QueryOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void getUser(Credential options,
      Callback<Account> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void getUserBatch(
      org.swellrt.beta.client.rest.operations.GetUserBatchOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.GetUserBatchOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void editUser(Account options,
      Callback<Account> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void listLogin(org.swellrt.beta.client.rest.operations.ListLoginOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.ListLoginOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void recoverPassword(
      CredentialData options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void password(CredentialData options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void getObjectNames(
      ObjectName options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void setObjectName(
      ObjectName options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void deleteObjectName(
      ObjectName options,
      Callback<org.swellrt.beta.client.rest.operations.params.Void> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public String getAppDomain() {
    return WAVE_DOMAIN;
  }

  //
  //
  //

  public SObject openSync(String id) {

    WaveId waveId = null;

    FakeWaveView wave;

    waveId = WaveId.of("local.net", id);

    if (waveId == null)
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant).build();
    else if (!objects.containsKey(waveId))
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant).with(waveId)
          .build();
    else {
      return objects.get(waveId);
    }

    SWaveNodeManager nodeManager = SWaveNodeManager.of(participant, idGenerator, "local.net", wave,
        null, nodeFactory);
    SWaveObject object = SWaveObject.materialize(nodeManager);

    objects.put(waveId, object);

    return objects.get(waveId);

  }

}
