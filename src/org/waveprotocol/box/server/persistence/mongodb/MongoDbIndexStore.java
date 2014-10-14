package org.waveprotocol.box.server.persistence.mongodb;



import com.google.gwt.dev.util.collect.HashMap;
import com.google.wave.api.SearchResult;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import org.waveprotocol.box.server.waveserver.QueryHelper;
import org.waveprotocol.box.server.waveserver.QueryHelper.OrderByValueType;
import org.waveprotocol.box.server.waveserver.TokenQueryType;
import org.waveprotocol.mod.model.WaveConversationUtils;
import org.waveprotocol.mod.model.WaveExtendedModel;
import org.waveprotocol.mod.model.WaveType;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MongoDbIndexStore {

  private static final Log LOG = Log.get(MongoDbIndexStore.class);

  private static final String INDEX_STORE_COLLECTION = "index";


  // Wave level fields
  public static final String FIELD_VIEW_FOR = "view_for";
  public static final String FIELD_WAVE_ID = "wave_id";
  public static final String FIELD_CREATOR = "creator";
  public static final String FIELD_VERSION = "version";
  public static final String FIELD_TYPE = "type";
  public static final String FIELD_TITLE = "title";
  public static final String FIELD_CREATED = "created_time";
  public static final String FIELD_LASTMOD = "last_mod_time";
  public static final String FIELD_PARTICIPANTS = "participants";

  // Wave contents level fields
  public static final String FIELD_CONTENT_CONVERSATION = "content_conversation";
  public static final String FIELD_CONTENT_CHAT = "content_chat";


  // Wave content-specific fields : conversation
  public static final String FIELD_SNIPPET = "snippet";

  public static final String FIELD_CONV_BLIPS = "doc_blips";
  public static final String FIELD_USER_BLIPS = "user_blips";
  public static final String FIELD_BLIP_ID = "blip_id";
  public static final String FIELD_BLIP_VERSION = "blip_version";

  // Wave content-specific fields : chat
  public static final String FIELD_CHAT_TITLE = "chat_title";
  public static final String FIELD_CHAT_PARTICIPANTS = "chat_participants";
  public static final String FIELD_LAST_MSG_TIME = "last_msg_time";
  public static final String FIELD_LAST_MSG_SENDER = "last_msg_sender";
  public static final String FIELD_LAST_MSG_ACK = "last_msg_ack";

  private final DB db;
  private final DBCollection store;
  private final WaveConversationUtils conversationUtils;

  public MongoDbIndexStore(DB database, WaveConversationUtils conversationUtils) {
    this.db = database;
    this.conversationUtils = conversationUtils;
    this.store = db.getCollection(INDEX_STORE_COLLECTION);


    LOG.info("Started mongoDB index store");
  }


  //
  // Index change methods
  //

  /**
   * We suppose to have only a conversation per wave, so any addition of users
   *
   * @param waveletName
   * @param addedParticipant
   */
  public void addParticipantIndexUpdate(WaveletName waveletName, ParticipantId addedParticipant) {

    if (!WaveExtendedModel.isContentWavelet(waveletName.waveletId)) return;

    BasicDBObject query = new BasicDBObject();
    query.put(FIELD_WAVE_ID, waveletName.waveId.serialise());

    // we create a copy of one index object for the new participant
    // the it will be updated with the new participant like others
    if (getIndexedWave(waveletName.waveId, addedParticipant) == null) {
      DBObject firstIndexObj = store.findOne(query);
      if (firstIndexObj != null) {
         duplicateIndex(addedParticipant, firstIndexObj);
      }
    }

    DBCursor cursor = store.find(query);
    try {
      while (cursor.hasNext()) {
        addParticipantIndexUpdate(addedParticipant, cursor.next());
      }
    } catch (Exception e) {
      LOG.severe("Error getting wave index", e);
    } finally {
      cursor.close();
    }
  }

  public void removeParticipantIndexUpdate(WaveletName waveletName, ParticipantId removedParticipant) {

    if (!WaveExtendedModel.isContentWavelet(waveletName.waveletId)) return;

    // First remove the index object for that user
    BasicDBObject query = new BasicDBObject();
    query.put(FIELD_WAVE_ID, waveletName.waveId.serialise());
    query.put(FIELD_VIEW_FOR, removedParticipant.getAddress());
    store.remove(query);

    // Second, update the rest of index objects
    query = new BasicDBObject();
    query.put(FIELD_WAVE_ID, waveletName.waveId.serialise());

    DBCursor cursor = store.find(query);
    try {
      while (cursor.hasNext()) {
        removeParticipantIndexUpdate(removedParticipant, cursor.next());
      }
    } catch (Exception e) {
      LOG.severe("Error getting wave index", e);
    } finally {
      cursor.close();
    }


  }


  protected void addParticipantIndexUpdate(ParticipantId addedParticipant, DBObject indexObj) {

    @SuppressWarnings("unchecked")
    List<String> participants = deserialize(List.class, indexObj.get(FIELD_PARTICIPANTS));

    String waveid = deserialize(String.class, indexObj.get(FIELD_WAVE_ID));
    String viewFor = deserialize(String.class, indexObj.get(FIELD_VIEW_FOR));

    if (!participants.contains(addedParticipant.getAddress()))
      participants.add(addedParticipant.getAddress());

    try {
      DBObject q = new BasicDBObject(FIELD_VIEW_FOR, viewFor).append(FIELD_WAVE_ID, waveid);
      store.update(q, indexObj);
    } catch (Exception e) {
      LOG.severe("Wave's participant index can't be updated " + waveid, e);
    }

  }


  protected void removeParticipantIndexUpdate(ParticipantId removedParticipant, DBObject indexObj) {


    @SuppressWarnings("unchecked")
    List<String> participants = deserialize(List.class, indexObj.get(FIELD_PARTICIPANTS));

    String waveid = deserialize(String.class, indexObj.get(FIELD_WAVE_ID));
    String viewFor = deserialize(String.class, indexObj.get(FIELD_VIEW_FOR));


    participants.remove(removedParticipant.getAddress());

    try {
      DBObject q = new BasicDBObject(FIELD_VIEW_FOR, viewFor).append(FIELD_WAVE_ID, waveid);
      store.update(q, indexObj);
    } catch (Exception e) {
      LOG.severe("Wave's participant index can't be updated " + waveid, e);
    }
  }


  public void indexWavelet(ReadableWaveletData waveletData) {
    for (ParticipantId p : waveletData.getParticipants()) {
      indexWavelet(waveletData, p);
    }
  }

  /**
   * Retrieves the wave index object from the database or null if it doesn't
   * exist.
   *
   * @param waveId
   * @param participantId
   * @return
   */
  protected DBObject getIndexedWave(WaveId waveId, ParticipantId participantId) {

    BasicDBObject query = new BasicDBObject();
    query.put(FIELD_VIEW_FOR, participantId.getAddress());
    query.put(FIELD_WAVE_ID, waveId.serialise());

    DBObject waveletView = null;

    DBCursor cursor = store.find(query);
    try {

      if (cursor.hasNext()) {
        waveletView = cursor.next();
      }
    } catch (Exception e) {
      LOG.severe("Error getting wave index", e);
    } finally {
      cursor.close();
    }

    return waveletView;
  }


  protected void indexWavelet(ReadableWaveletData waveletData, ParticipantId participantId) {

    LOG.info("Actual indexing of wavelet "
        + WaveletName.of(waveletData.getWaveId(), waveletData.getWaveletId()));

    // Avoid further processing if it's an user data wavelet for another
    // participant
    if (WaveExtendedModel.isUserDataWavelet(waveletData.getWaveletId())
        && !participantId.equals(waveletData.getCreator())) return;

    DBObject waveView = getIndexedWave(waveletData.getWaveId(), participantId);
    WaveType waveType = WaveType.fromWaveId(waveletData.getWaveId());
    boolean isNew = waveView == null;

    if (isNew) {
      // Create an empty DBObject to place the wave view
      waveView = new BasicDBObject();
      waveView.put(FIELD_VIEW_FOR, participantId.getAddress());
      waveView.put(FIELD_WAVE_ID, (waveletData.getWaveId().serialise()));
      waveView.put(FIELD_TYPE, WaveType.serialize(waveType));
    }

    // if is the content wavelet
    if (WaveExtendedModel.isContentWavelet(waveletData.getWaveletId())) {

      // creator
      waveView.put(FIELD_CREATOR, waveletData.getCreator().getAddress());

      // participants
      waveView.put(FIELD_PARTICIPANTS, serialize(waveletData.getParticipants()));

      // creation timestamp
      waveView.put(FIELD_CREATED, waveletData.getCreationTime());

      // lastmod timestamp
      waveView.put(FIELD_LASTMOD, waveletData.getLastModifiedTime());

      // wave's type
      if (waveType.equals(WaveType.CONVERSATION)) {

        // title and snippet
        String title = conversationUtils.getWaveletConversationTitle(waveletData);
        waveView.put(FIELD_TITLE, title);

        String snippet = conversationUtils.getWaveletConversationSnippet(waveletData);
        waveView.put(FIELD_SNIPPET, snippet);

        // rest of conversation specific data
        DBObject contentConversation = (DBObject) waveView.get(FIELD_CONTENT_CONVERSATION);

        if (contentConversation == null) {
          contentConversation = new BasicDBObject();
        }

        setConversationContentData(contentConversation, waveletData, participantId);

        waveView.put(FIELD_CONTENT_CONVERSATION, contentConversation);

      } else if (waveType.equals(WaveType.CHAT)) {

        // title and snippet
        waveView.put(FIELD_TITLE, "Chat");

        // rest of chat specific data
        waveView.put(FIELD_TITLE, "");
      }

      // if it is a personal wavelet
    } else if (WaveExtendedModel.isUserDataWavelet(waveletData.getWaveletId())) {


      // wave's type
      if (waveType.equals(WaveType.CONVERSATION)) {

        DBObject contentConversation = (DBObject) waveView.get(FIELD_CONTENT_CONVERSATION);

        if (contentConversation == null) {
          contentConversation = new BasicDBObject();
        }

        // conversation
        setConversationUserData(contentConversation, waveletData, participantId);

        waveView.put(FIELD_CONTENT_CONVERSATION, contentConversation);


      } else if (waveType.equals(WaveType.CHAT)) {

        // chat
      }


    // Not a known wavelet type
    } else {
      LOG.info("Wavelet can't be indexed: "
          + WaveletName.of(waveletData.getWaveId(), waveletData.getWaveletId()));
      return;
    }

    try {

      // Store
      if (isNew)
        store.insert(waveView);
      else {
        DBObject q =
            new BasicDBObject(FIELD_VIEW_FOR, participantId.getAddress()).append(FIELD_WAVE_ID,
                waveletData
                .getWaveId().serialise());
        store.update(q, waveView);
      }

    } catch (Exception e) {
      LOG.severe(
          "Wavelet can't be indexed  "
              + WaveletName.of(waveletData.getWaveId(), waveletData.getWaveletId()), e);
    }

  }

  /**
   * Create a wave index view for a new participant based on the index of
   * another user. A clean up process is done before persist.
   *
   * @param participant
   * @param indexObj
   */
  protected void duplicateIndex(ParticipantId participant, DBObject indexObj) {

    indexObj.put(FIELD_VIEW_FOR, participant.getAddress());
    indexObj.removeField("_id");

    // Remove all participant specific content. By now
    indexObj.removeField(FIELD_CONTENT_CONVERSATION);
    indexObj.removeField(FIELD_CONTENT_CHAT);

    store.insert(indexObj);
  }


  /**
   * Add conversation specific info into the provided DB object: - List of doc's
   * blip ids and versions
   *
   * @param conversationObject the DB object where to put the info
   * @param waveletData
   * @param participantId
   */
  private void setConversationContentData(DBObject conversationObject,
      ReadableWaveletData waveletData,
      ParticipantId participantId) {

    Map<String, Long> docBlips = conversationUtils.getConversationBlips(waveletData);
    BasicDBList docBlipsObj = new BasicDBList();

    for (Entry<String, Long> e :docBlips.entrySet()) {
      DBObject blipObj = new BasicDBObject();
      blipObj.put(FIELD_BLIP_ID, e.getKey());
      blipObj.put(FIELD_BLIP_VERSION, e.getValue());
      docBlipsObj.add(blipObj);
    }

    conversationObject.put(FIELD_CONV_BLIPS, docBlipsObj);

  }

  /**
   * Add user specific data of a conversation in the provided DB object: - List
   * of user's blip id's a versions
   *
   * @param conversationObject
   * @param waveletData
   * @param participantId
   */
  private void setConversationUserData(DBObject conversationObject,
      ReadableWaveletData waveletData, ParticipantId participantId) {


    Map<String, Long> docBlips = conversationUtils.getUserDataBlips(waveletData);
    BasicDBList docBlipsObj = new BasicDBList();

    for (Entry<String, Long> e : docBlips.entrySet()) {
      DBObject blipObj = new BasicDBObject();
      blipObj.put(FIELD_BLIP_ID, e.getKey());
      blipObj.put(FIELD_BLIP_VERSION, e.getValue());
      docBlipsObj.add(blipObj);
    }

    conversationObject.put(FIELD_USER_BLIPS, docBlipsObj);


  }


  //
  // Query index methods
  //

  /**
   *
   *
   * @param user
   * @param queryMap
   * @return
   */
  public SearchResult queryIndex(ParticipantId user, Map<TokenQueryType, Set<String>> queryMap,
      int skip, int limit, SearchResult result) {

    DBObject sort = new BasicDBObject();

    BasicDBObject query = new BasicDBObject();
    query.put(FIELD_VIEW_FOR, user.getAddress());

    if (queryMap != null) {

      if (queryMap.containsKey(TokenQueryType.CREATOR)) {
        query.put(FIELD_CREATOR, queryMap.get(TokenQueryType.CREATOR).iterator().next());
      }

      if (queryMap.containsKey(TokenQueryType.WITH)) {
        for (String participant : queryMap.get(TokenQueryType.CREATOR))
          query.put(FIELD_PARTICIPANTS, participant);
      }


      if (queryMap.containsKey(TokenQueryType.ORDERBY)) {

        for (String sortToken : queryMap.get(TokenQueryType.ORDERBY)) {

          OrderByValueType orderBy = QueryHelper.OrderByValueType.fromToken(sortToken);

          switch (orderBy) {
            case CREATEDASC:
              sort.put(FIELD_CREATED, 1);
              break;
            case CREATEDDESC:
              sort.put(FIELD_CREATED, -1);
              break;
            case DATEASC:
              sort.put(FIELD_LASTMOD, 1);
              break;
            case DATEDESC:
              sort.put(FIELD_LASTMOD, -1);
              break;
            case CREATORASC:
              sort.put(FIELD_CREATOR, 1);
              break;
            case CREATORDESC:
              sort.put(FIELD_CREATOR, -1);
              break;
          }

        }
      }
 else
        sort.put(FIELD_LASTMOD, -1);
    }

    DBCursor cursor = store.find(query).sort(sort).skip(skip).limit(skip);


    if (result == null) result = new SearchResult("");

    try {
      while (cursor.hasNext()) {
        result.addDigest(generateDigester(cursor.next()));
      }
    } finally {
      cursor.close();
    }



    return result;
  }


  //
  // Utility methods
  //


  /**
   * A more secure method to deserialize from mongoDB
   *
   * @param clazz
   * @param obj
   * @return
   */
  protected <T> T deserialize(Class<T> clazz, Object obj) {


    if (clazz.isInstance(obj)) return clazz.cast(obj);

    try {
      T empty = clazz.newInstance();
      return empty;
    } catch (InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }

    return null;
  }


  @SuppressWarnings("rawtypes")
  protected List deserializeList(Object obj) {


    if (List.class.isInstance(obj)) return List.class.cast(obj);


    ArrayList empty = new ArrayList();
    return empty;

  }

  protected SearchResult.Digest generateDigester(DBObject obj) {

    int totalBlipCount = 0;
    int unreadBlipCount = 0;

    if (WaveType.deserialize((String) obj.get(FIELD_TYPE)).equals(WaveType.CONVERSATION)) {

      DBObject contentObj = (DBObject) obj.get(FIELD_CONTENT_CONVERSATION);

      if (contentObj != null) {
        Map<String, Long> convBlips =
            deserializeBlipListObject(deserialize(BasicDBList.class,
                contentObj.get(FIELD_CONV_BLIPS)));

        Map<String, Long> userBlips =
            deserializeBlipListObject(deserialize(BasicDBList.class,
                contentObj.get(FIELD_USER_BLIPS)));

        totalBlipCount = convBlips.size();
        unreadBlipCount = conversationUtils.getNotReadBlips(convBlips, userBlips);
      }
    }



    @SuppressWarnings("unchecked")
    SearchResult.Digest digest =
        new SearchResult.Digest(deserialize(String.class, obj.get(FIELD_TITLE)), deserialize(
            String.class, obj.get(FIELD_SNIPPET)), (String) obj.get(FIELD_WAVE_ID),
            deserializeList(obj.get(FIELD_PARTICIPANTS)), (Long) obj.get(FIELD_LASTMOD),
            (Long) obj.get(FIELD_CREATED), unreadBlipCount, totalBlipCount);

    return digest;
  }

  protected List<String> serialize(Set<ParticipantId> participants) {
    ArrayList<String> addresses = new ArrayList<String>(participants.size());
    for (ParticipantId p : participants)
      addresses.add(p.getAddress());
    return addresses;
  }

  protected Set<ParticipantId> deserializeParticipantId(List<String> addresses) {
    Set<ParticipantId> participants = new HashSet<ParticipantId>();
    for (String s : addresses)
      participants.add(ParticipantId.ofUnsafe(s));
    return participants;
  }

  protected Map<String, Long> deserializeBlipListObject(BasicDBList blipsObj) {

    Map<String, Long> map = new HashMap<String, Long>();
    for (Object o : blipsObj) {
      DBObject dbo = (DBObject) o;
      String blipId = (String) dbo.get(FIELD_BLIP_ID);
      Long blipVersion = (Long) dbo.get(FIELD_BLIP_VERSION);
      map.put(blipId, blipVersion);
    }

    return map;

  }


}
