/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.persistence.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.BasicBSONList;
import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.box.attachment.AttachmentProto;
import org.waveprotocol.box.attachment.proto.AttachmentMetadataProtoImpl;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.account.SecretToken;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.SignerInfoStore;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.wave.api.Context;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

/**
 * <b>CertPathStore:</b><br/>
 * <i>Collection(signerInfo):</i>
 * <ul>
 * <li>_id : signerId byte array.</li>
 * <li>protoBuff : byte array representing the protobuff message of a
 * {@link ProtocolSignerInfo}.</li>
 * </ul>
 * <p>
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author josephg@gmail.com (Joseph Gentle)
 *
 */
public final class MongoDbStore implements SignerInfoStore, AttachmentStore, AccountStore {

  private static final String ACCOUNT_COLLECTION = "account";
  private static final String ACCOUNT_HUMAN_DATA_FIELD = "human";
  private static final String ACCOUNT_ROBOT_DATA_FIELD = "robot";

  private static final String HUMAN_PASSWORD_FIELD = "passwordDigest";

  private static final String PASSWORD_DIGEST_FIELD = "digest";
  private static final String PASSWORD_SALT_FIELD = "salt";

  private static final String ROBOT_URL_FIELD = "url";
  private static final String ROBOT_SECRET_FIELD = "secret";
  private static final String ROBOT_CAPABILITIES_FIELD = "capabilities";
  private static final String ROBOT_VERIFIED_FIELD = "verified";

  private static final String CAPABILITIES_VERSION_FIELD = "version";
  private static final String CAPABILITIES_HASH_FIELD = "capabilitiesHash";
  private static final String CAPABILITIES_CAPABILITIES_FIELD = "capabilities";
  private static final String CAPABILITY_CONTEXTS_FIELD = "contexts";
  private static final String CAPABILITY_FILTER_FIELD = "filter";

  private static final Logger LOG = Logger.getLogger(MongoDbStore.class.getName());
  private static final String EMAIL_FIELD = "email";
  private static final String TOKEN_FIELD = "token";
  private static final String TOKEN_DATE_FIELD = "token_date";

  private static final String ACCOUNT_AVATAR_FILE = "avatarFile";
  private static final String ACCOUNT_LOCALE = "locale";
  private static final String NAME_FIELD = "name";

  private final MongoDatabase database;
  private final GridFSBucket attachmentGrid;
  private final GridFSBucket thumbnailGrid;
  private final GridFSBucket metadataGrid;

  MongoDbStore(MongoDatabase database) {
    this.database = database;
    attachmentGrid = GridFSBuckets.create(database, "attachments");
    thumbnailGrid = GridFSBuckets.create(database, "thumbnails");
    metadataGrid = GridFSBuckets.create(database, "metadata");
  }

  @Override
  public void initializeSignerInfoStore() throws PersistenceException {
    // Nothing to initialize
  }

  private static Bson filterSignerInfoBy(byte[] signerId) {
    return Filters.eq("_id", signerId);
  }

  @Override
  public SignerInfo getSignerInfo(byte[] signerId) {

    BasicDBObject signerInfoDBObject = getSignerInfoCollection().find(filterSignerInfoBy(signerId)).first();
    // Sub-class contract specifies return null when not found
    SignerInfo signerInfo = null;

    if (signerInfoDBObject != null) {
      byte[] protobuff = (byte[]) signerInfoDBObject.get("protoBuff");
      try {
        signerInfo = new SignerInfo(ProtocolSignerInfo.parseFrom(protobuff));
      } catch (InvalidProtocolBufferException e) {
        LOG.log(Level.SEVERE, "Couldn't parse the protobuff stored in MongoDB: " + protobuff, e);
      } catch (SignatureException e) {
        LOG.log(Level.SEVERE, "Couldn't parse the certificate chain or domain properly", e);
      }
    }
    return signerInfo;
  }

  @Override
  public void putSignerInfo(ProtocolSignerInfo protocolSignerInfo) throws SignatureException {
    SignerInfo signerInfo = new SignerInfo(protocolSignerInfo);
    byte[] signerId = signerInfo.getSignerId();

    // Not using a modifier here because rebuilding the object is not a lot of
    // work.
    BasicDBObject signerInfoDBObject = new BasicDBObject("_id", signerId);
    signerInfoDBObject.put("protoBuff", protocolSignerInfo.toByteArray());

    getSignerInfoCollection().replaceOne(filterSignerInfoBy(signerId), signerInfoDBObject,
        new UpdateOptions().upsert(true));
  }

  /**
   * Returns an instance of {@link DBCollection} for storing SignerInfo.
   */
  private MongoCollection<BasicDBObject> getSignerInfoCollection() {
    return database.getCollection("signerInfo", BasicDBObject.class);
  }



  // *********** Attachments.

  @Override
  public AttachmentData getAttachment(AttachmentId attachmentId) {

    GridFSFile gridFile = getAttachmentGridFSFile(attachmentId);

    if (gridFile == null)
      return null;


    return fileToAttachmentData(gridFile);
  }

  @Override
  public void storeAttachment(AttachmentId attachmentId, InputStream dataInputStream)
      throws IOException {

    GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(1024)
        .metadata(new Document("type", "attachment").append("id", attachmentId.serialise()));

    attachmentGrid.uploadFromStream(attachmentId.serialise(), dataInputStream, options);
  }

  @Override
  public void deleteAttachment(AttachmentId attachmentId) {
    GridFSFile attachmentFile = getAttachmentGridFSFile(attachmentId);
    if (attachmentFile != null)
      attachmentGrid.delete(attachmentFile.getObjectId());

    GridFSFile thumbnailFile = getThumnailGridFSFile(attachmentId);
    if (thumbnailFile != null)
      thumbnailGrid.delete(thumbnailFile.getObjectId());

    GridFSFile metadataFile = getMetadataGridFSFile(attachmentId);
    if (metadataFile != null)
      metadataGrid.delete(metadataFile.getObjectId());
  }

  private GridFSFile getAttachmentGridFSFile(AttachmentId attachmentId) {
    return attachmentGrid.find(eq("metadata.id", attachmentId.serialise())).first();
  }

  private GridFSFile getMetadataGridFSFile(AttachmentId attachmentId) {
    return metadataGrid.find(eq("metadata.id", attachmentId.serialise())).first();
  }

  private GridFSFile getThumnailGridFSFile(AttachmentId attachmentId) {
    return thumbnailGrid.find(eq("metadata.id", attachmentId.serialise())).first();
  }

  @Override
  public AttachmentMetadata getMetadata(AttachmentId attachmentId) throws IOException {
    GridFSFile metadataFile = getMetadataGridFSFile(attachmentId);

    if (metadataFile == null)
      return null;

    try (GridFSDownloadStream metadataStream = metadataGrid
        .openDownloadStream(metadataFile.getObjectId())) {

      if (metadataStream == null) {
        return null;
      }

      AttachmentProto.AttachmentMetadata protoMetadata = AttachmentProto.AttachmentMetadata
          .parseFrom(metadataStream);

      return new AttachmentMetadataProtoImpl(protoMetadata);

    } catch (MongoException e) {
      throw new IOException(e);
    }

  }

  @Override
  public AttachmentData getThumbnail(AttachmentId attachmentId) throws IOException {
    GridFSFile thumbFile = getThumnailGridFSFile(attachmentId);
    return fileToAttachmentData(thumbFile);
  }

  @Override
  public void storeMetadata(AttachmentId attachmentId, AttachmentMetadata metaData)
      throws IOException {
    AttachmentMetadataProtoImpl proto = new AttachmentMetadataProtoImpl(metaData);
    byte[] bytes = proto.getPB().toByteArray();

    GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(255)
        .metadata(new Document("type", "metadata").append("id", attachmentId.serialise()));

    metadataGrid.uploadFromStream(attachmentId.serialise(), new ByteArrayInputStream(bytes),
        options);
  }

  @Override
  public void storeThumbnail(AttachmentId attachmentId, InputStream dataInputStream)
      throws IOException {

    GridFSUploadOptions options = new GridFSUploadOptions()
        .chunkSizeBytes(1024)
        .metadata(new Document("type", "thumbnail").append("id", attachmentId.serialise()));

    thumbnailGrid.uploadFromStream(attachmentId.serialise(), dataInputStream,
        options);
  }


  private AttachmentData fileToAttachmentData(final GridFSFile attachmentFile) {
    if (attachmentFile == null) {
      return null;
    } else {
      return new AttachmentData() {

        @Override
        public InputStream getInputStream() throws IOException {
          return attachmentGrid.openDownloadStream(attachmentFile.getObjectId());
        }

        @Override
        public long getSize() {
          return attachmentFile.getLength();
        }
      };
    }
  }

  // ******** AccountStore

  private static Bson filterAccountBy(ParticipantId id) {
    return Filters.eq("_id", id.getAddress());
  }

  private static Bson filterAccountByEmail(String email) {
    return Filters.eq(ACCOUNT_HUMAN_DATA_FIELD + "." + EMAIL_FIELD, email);
  }

  private BasicDBObject getAccountDBObject(ParticipantId id) {
    return getAccountCollection().find(filterAccountBy(id)).first();
  }

  @Override
  public void initializeAccountStore() throws PersistenceException {
    // TODO: Sanity checks not handled by MongoDBProvider???
  }

  @Override
  public AccountData getAccount(ParticipantId id) {
    BasicDBObject accountDBObject = getAccountDBObject(id);

    if (accountDBObject == null) {
      return null;
    }

    return accountFromQueryResult(accountDBObject, id);
  }

  @Override
  public void putAccount(AccountData account) {

    BasicDBObject object = new BasicDBObject("_id", account.getId().getAddress());

    if (account.isHuman()) {
      object.put(ACCOUNT_HUMAN_DATA_FIELD, humanToObject(account.asHuman()));
    } else if (account.isRobot()) {
      object.put(ACCOUNT_ROBOT_DATA_FIELD, robotToObject(account.asRobot()));
    } else {
      throw new IllegalStateException("Account is neither a human nor a robot");
    }

    getAccountCollection().replaceOne(
        filterAccountBy(account.getId()),
        object,
        new UpdateOptions().upsert(true));
  }

  @Override
  public void removeAccount(ParticipantId id) {
    getAccountCollection().deleteOne(filterAccountBy(id));
  }


  private MongoCollection<BasicDBObject> getAccountCollection() {
    return database.getCollection(ACCOUNT_COLLECTION, BasicDBObject.class);
  }

  // ****** HumanAccountData serialization

  private DBObject humanToObject(HumanAccountData account) {
    DBObject object = new BasicDBObject();

    PasswordDigest digest = account.getPasswordDigest();
    if (digest != null) {
      DBObject digestObj = new BasicDBObject();
      digestObj.put(PASSWORD_SALT_FIELD, digest.getSalt());
      digestObj.put(PASSWORD_DIGEST_FIELD, digest.getDigest());

      object.put(HUMAN_PASSWORD_FIELD, digestObj);
    }

    if (account.getEmail() != null) {
      object.put(EMAIL_FIELD, account.getEmail());
    }

    if (account.getRecoveryToken() != null) {
      object.put(TOKEN_FIELD, account.getRecoveryToken().getToken());
      object.put(TOKEN_DATE_FIELD, account.getRecoveryToken().getExpirationDate());
    }

    if (account.getLocale() != null) {
      object.put(ACCOUNT_LOCALE, account.getLocale());
    }

    if (account.getAvatarFileName() != null) {
      object.put(ACCOUNT_AVATAR_FILE, account.getAvatarFileId());
    }

    if (account.getName() != null) {
      object.put(NAME_FIELD, account.getName());
    }


    return object;
  }

  private HumanAccountData objectToHuman(ParticipantId id, DBObject object) {
    PasswordDigest passwordDigest = null;
    String email = null;
    SecretToken token = null;
    String locale = null;
    String avatarFileName = null;
    String name = null;

    DBObject digestObj = (DBObject) object.get(HUMAN_PASSWORD_FIELD);
    if (digestObj != null) {
      byte[] salt = (byte[]) digestObj.get(PASSWORD_SALT_FIELD);
      byte[] digest = (byte[]) digestObj.get(PASSWORD_DIGEST_FIELD);
      passwordDigest = PasswordDigest.from(salt, digest);
    }
    email = (String) object.get(EMAIL_FIELD);
    token = new SecretToken((String) object.get(TOKEN_FIELD),
        (java.util.Date) object.get(TOKEN_DATE_FIELD));


    locale = (String) object.get(ACCOUNT_LOCALE);
    avatarFileName = (String) object.get(ACCOUNT_AVATAR_FILE);
    name = (String) object.get(NAME_FIELD);

    return new HumanAccountDataImpl(id, passwordDigest, email, token, locale, avatarFileName, name);
  }

  // ****** RobotAccountData serialization

  private DBObject robotToObject(RobotAccountData account) {
    return new BasicDBObject()
        .append(ROBOT_URL_FIELD, account.getUrl())
        .append(ROBOT_SECRET_FIELD, account.getConsumerSecret())
        .append(ROBOT_CAPABILITIES_FIELD, capabilitiesToObject(account.getCapabilities()))
        .append(ROBOT_VERIFIED_FIELD, account.isVerified());
  }

  private DBObject capabilitiesToObject(RobotCapabilities capabilities) {
    if (capabilities == null) {
      return null;
    }

    BasicDBObject capabilitiesObj = new BasicDBObject();
    for (Capability capability : capabilities.getCapabilitiesMap().values()) {
      BasicBSONList contexts = new BasicBSONList();
      for (Context c : capability.getContexts()) {
        contexts.add(c.name());
      }
      capabilitiesObj.put(capability.getEventType().name(),
          new BasicDBObject()
              .append(CAPABILITY_CONTEXTS_FIELD, contexts)
              .append(CAPABILITY_FILTER_FIELD, capability.getFilter()));
    }

    BasicDBObject object =
        new BasicDBObject()
            .append(CAPABILITIES_CAPABILITIES_FIELD, capabilitiesObj)
            .append(CAPABILITIES_HASH_FIELD, capabilities.getCapabilitiesHash())
            .append(CAPABILITIES_VERSION_FIELD, capabilities.getProtocolVersion().name());

    return object;
  }

  private AccountData objectToRobot(ParticipantId id, DBObject robot) {
    String url = (String) robot.get(ROBOT_URL_FIELD);
    String secret = (String) robot.get(ROBOT_SECRET_FIELD);
    RobotCapabilities capabilities =
        objectToCapabilities((DBObject) robot.get(ROBOT_CAPABILITIES_FIELD));
    boolean verified = (Boolean) robot.get(ROBOT_VERIFIED_FIELD);
    return new RobotAccountDataImpl(id, url, secret, capabilities, verified);
  }

  @SuppressWarnings("unchecked")
  private RobotCapabilities objectToCapabilities(DBObject object) {
    if (object == null) {
      return null;
    }

    Map<String, Object> capabilitiesObj =
	(Map<String, Object>) object.get(CAPABILITIES_CAPABILITIES_FIELD);
    Map<EventType, Capability> capabilities = CollectionUtils.newHashMap();

    for (Entry<String, Object> capability : capabilitiesObj.entrySet()) {
      EventType eventType = EventType.valueOf(capability.getKey());
      List<Context> contexts = CollectionUtils.newArrayList();
      DBObject capabilityObj = (DBObject) capability.getValue();
      DBObject contextsObj = (DBObject) capabilityObj.get(CAPABILITY_CONTEXTS_FIELD);
      for (String contextId : contextsObj.keySet()) {
        contexts.add(Context.valueOf((String) contextsObj.get(contextId)));
      }
      String filter = (String) capabilityObj.get(CAPABILITY_FILTER_FIELD);

      capabilities.put(eventType, new Capability(eventType, contexts, filter));
    }

    String capabilitiesHash = (String) object.get(CAPABILITIES_HASH_FIELD);
    ProtocolVersion version =
        ProtocolVersion.valueOf((String) object.get(CAPABILITIES_VERSION_FIELD));

    return new RobotCapabilities(capabilities, capabilitiesHash, version);
  }

  @Override
  public List<AccountData> getAccountByEmail(String email) throws PersistenceException {

    Preconditions.checkNotNull(email, "Email can't be null");
    final ArrayList<AccountData> accounts = new ArrayList<AccountData>();
    getAccountCollection().find(filterAccountByEmail(email)).forEach(new Block<BasicDBObject>() {

      @Override
      public void apply(BasicDBObject t) {
        accounts.add(accountFromQueryResult(t, null));
      }

    });
    return accounts;

  }

  private AccountData accountFromQueryResult(DBObject result, ParticipantId id) {

    if (id == null) {
      id = new ParticipantId((String) result.get("_id"));
    }

    DBObject human = (DBObject) result.get(ACCOUNT_HUMAN_DATA_FIELD);
    if (human != null) {
      return objectToHuman(id, human);
    }

    DBObject robot = (DBObject) result.get(ACCOUNT_ROBOT_DATA_FIELD);
    if (robot != null) {
      return objectToRobot(id, robot);
    }

    throw new IllegalStateException("DB object contains neither a human nor a robot");

  }

}
