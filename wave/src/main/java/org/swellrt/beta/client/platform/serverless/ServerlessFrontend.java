package org.swellrt.beta.client.platform.serverless;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.GroupsFrontend;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.client.rest.operations.params.CredentialData;
import org.swellrt.beta.client.rest.operations.params.ObjectId;
import org.swellrt.beta.client.rest.operations.params.ObjectName;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.client.wave.DummyLazyContentFactory;
import org.swellrt.beta.client.wave.SWaveDocuments;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.presence.SSession;
import org.swellrt.beta.model.presence.SSessionManager;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.wave.LazyContentDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.MuteDocumentFactory;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Serverless")
public class ServerlessFrontend implements ServiceFrontend {

  private static final String WAVE_DOMAIN = "local.net";

  private ParticipantId participant = ParticipantId.ofUnsafe("fake@local.net");
  private SSession session = new SSession("fake-session-id", participant, RgbColor.WHITE,
      "Fake Name", "fakey");
  private SSessionManager sessionProvider = new SSessionManager(session);

  private IdGenerator idGenerator = new IdGeneratorImpl("local.net", new IdGeneratorImpl.Seed() {
    @Override
    public String get() {
      return "ABCDEFGHIK"; // the seed :D
    }
  });




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

    SWaveDocuments<LazyContentDocument> docRegistry = SWaveDocuments.create(
        new DummyLazyContentFactory(Editor.ROOT_REGISTRIES),

        new MuteDocumentFactory(new SchemaProvider() {

          @Override
          public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
            return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
          }
        }));

    WaveId waveId = null;

    FakeWaveView wave;

    waveId = WaveId.of("local.net", id);

    if (waveId == null)
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant)
          .with(docRegistry).build();
    else if (!objects.containsKey(waveId))
      wave = BasicFactories.fakeWaveViewBuilder().with(idGenerator).with(participant)
          .with(docRegistry).with(waveId)
          .build();
    else {
      return objects.get(waveId);
    }

    SWaveNodeManager nodeManager = SWaveNodeManager.create(sessionProvider, idGenerator,
        "local.net", wave, null,
        docRegistry);
    SWaveObject object = nodeManager.getSWaveObject();

    objects.put(waveId, object);

    return objects.get(waveId);

  }

  @Override
  public GroupsFrontend groups() {
    throw new IllegalStateException("Not implemented yet");
  }

}
