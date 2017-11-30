package org.swellrt.beta.client.platform.serverless;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.platform.web.editor.STextRemoteWeb;
import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.CreateUserOperation.Options;
import org.swellrt.beta.client.rest.operations.CreateUserOperation.Response;
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
  public void createUser(Options options, Callback<Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void login(org.swellrt.beta.client.rest.operations.LoginOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.LoginOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void logout(org.swellrt.beta.client.rest.operations.LogoutOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.LogoutOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void resume(org.swellrt.beta.client.rest.operations.ResumeOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.ResumeOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void open(org.swellrt.beta.client.rest.operations.OpenOperation.Options options,
      Callback<SObject> callback) {

    WaveId waveId = null;

    FakeWaveView wave;

    try {
      waveId = WaveId.of("local.net", options.getId());
    } catch (IllegalArgumentException e) {
    } catch (Exception e) {

    }

    if (waveId == null)
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant).build();
    else if (!objects.containsKey(waveId))
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant).with(waveId)
          .build();
    else {
      callback.onSuccess(objects.get(waveId));
      return;
    }

    SWaveNodeManager nodeManager = SWaveNodeManager.of(participant, idGenerator, "local.net", wave,
        null, nodeFactory);
    SWaveObject object = SWaveObject.materialize(nodeManager);

    objects.put(waveId, object);

    callback.onSuccess(objects.get(waveId));

  }

  @Override
  public void close(org.swellrt.beta.client.rest.operations.CloseOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.CloseOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void query(org.swellrt.beta.client.rest.operations.QueryOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.QueryOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void getUser(org.swellrt.beta.client.rest.operations.GetUserOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.GetUserOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void getUserBatch(
      org.swellrt.beta.client.rest.operations.GetUserBatchOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.GetUserBatchOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void editUser(org.swellrt.beta.client.rest.operations.EditUserOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.EditUserOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void listLogin(org.swellrt.beta.client.rest.operations.ListLoginOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.ListLoginOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void recoverPassword(
      org.swellrt.beta.client.rest.operations.PasswordRecoverOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.PasswordRecoverOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void password(org.swellrt.beta.client.rest.operations.PasswordOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.PasswordOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void getObjectNames(
      org.swellrt.beta.client.rest.operations.naming.GetNamesOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.naming.GetNamesOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void setObjectName(
      org.swellrt.beta.client.rest.operations.naming.SetNameOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.naming.SetNameOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void deleteObjectName(
      org.swellrt.beta.client.rest.operations.naming.DeleteNameOperation.Options options,
      Callback<org.swellrt.beta.client.rest.operations.naming.DeleteNameOperation.Response> callback) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public String getAppDomain() {
    return WAVE_DOMAIN;
  }

}
