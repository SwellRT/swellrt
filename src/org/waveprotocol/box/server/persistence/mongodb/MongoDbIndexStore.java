package org.waveprotocol.box.server.persistence.mongodb;



import com.google.wave.api.SearchResult;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import org.waveprotocol.box.server.waveserver.QueryHelper;
import org.waveprotocol.box.server.waveserver.QueryHelper.OrderByValueType;
import org.waveprotocol.box.server.waveserver.TokenQueryType;
import org.waveprotocol.wave.model.extended.WaveConversationUtils;
import org.waveprotocol.wave.model.extended.WaveExtendedModel;
import org.waveprotocol.wave.model.extended.WaveType;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  public static final String FIELD_CONTENT_DOC = "content_doc";

  public static final String FIELD_CONTENT_DOC_ID = "doc_id";


  // Wave content-specific fields : conversation
  public static final String FIELD_BLIP_TOTAL_COUNT = "blip_total_count";
  public static final String FIELD_BLIP_UNREAD_COUNT = "blip_unread_count";
  public static final String FIELD_SNIPPET = "snippet";

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

  public void indexWavelet(ReadableWaveletData waveletData) {
    for (ParticipantId p : waveletData.getParticipants()) {
      indexWavelet(waveletData, p);
    }
  }



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
        String title = conversationUtils.getConversationTitle(waveletData);
        waveView.put(FIELD_TITLE, title);

        String snippet = conversationUtils.getWaveletConversationSnippet(waveletData);
        waveView.put(FIELD_SNIPPET, snippet);

        // rest of conversation specific data
        setConversationContentData(waveView, waveletData, participantId);


      } else if (waveType.equals(WaveType.CHAT)) {

        // title and snippet
        // TODO specific title of chat

        // rest of chat specific data
        // TODO specific content of chat
      }

      // if it is a personal wavelet
    } else if (WaveExtendedModel.isUserDataWavelet(waveletData.getWaveletId())) {


      // wave's type
      if (waveType.equals(WaveType.CONVERSATION)) {

        // conversation
        setConversationUserData(waveView, waveletData, participantId);

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


  private void setConversationContentData(DBObject conversationObject,
      ReadableWaveletData waveletData,
      ParticipantId participantId) {

  }

  private void setConversationUserData(DBObject conversationObject,
      ReadableWaveletData waveletData, ParticipantId participantId) {

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

  protected SearchResult.Digest generateDigester(DBObject obj) {

    @SuppressWarnings({"rawtypes", "unchecked"})
    SearchResult.Digest digest =
        new SearchResult.Digest(deserialize(String.class, obj.get(FIELD_TITLE)), deserialize(
            String.class, obj.get(FIELD_SNIPPET)),
 (String) obj.get(FIELD_WAVE_ID),
            (List) obj.get(FIELD_PARTICIPANTS),
            (Long) obj.get(FIELD_LASTMOD), (Long) obj.get(FIELD_CREATED),
            0, 0);

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



}
