package org.waveprotocol.box.server.persistence.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

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
import java.util.Set;

public class MongoDbIndexStore {

  private static final Log LOG = Log.get(MongoDbIndexStore.class);

  private static final String INDEX_STORE_COLLECTION = "index";


  // Wave level fields
  private static final String FIELD_VIEW_FOR = "view_for";
  private static final String FIELD_WAVE_ID = "wave_id";
  private static final String FIELD_CREATOR = "creator";
  private static final String FIELD_VERSION = "version";
  private static final String FIELD_TYPE = "type";
  private static final String FIELD_TITLE = "title";
  private static final String FIELD_CREATED = "created_time";
  private static final String FIELD_LASTMOD = "last_mod_time";
  private static final String FIELD_PARTICIPANTS = "participants";

  // Wave contents level fields
  private static final String FIELD_CONTENT_CONVERSATION = "content_conversation";
  private static final String FIELD_CONTENT_CHAT = "content_chat";
  private static final String FIELD_CONTENT_DOC = "content_doc";

  private static final String FIELD_CONTENT_DOC_ID = "doc_id";


  // Wave content-specific fields : conversation
  private static final String FIELD_BLIP_TOTAL_COUNT = "blip_total_count";
  private static final String FIELD_BLIP_UNREAD_COUNT = "blip_unread_count";
  private static final String FIELD_SNIPPET = "snippet";

  // Wave content-specific fields : chat
  private static final String FIELD_CHAT_TITLE = "chat_title";
  private static final String FIELD_CHAT_PARTICIPANTS = "chat_participants";
  private static final String FIELD_LAST_MSG_TIME = "last_msg_time";
  private static final String FIELD_LAST_MSG_SENDER = "last_msg_sender";
  private static final String FIELD_LAST_MSG_ACK = "last_msg_ack";

  private final DB db;
  private final DBCollection store;
  private final WaveConversationUtils conversationUtils;

  public MongoDbIndexStore(DB database, WaveConversationUtils conversationUtils) {
    this.db = database;
    this.conversationUtils = conversationUtils;
    this.store = db.getCollection(INDEX_STORE_COLLECTION);
  }


  public void indexWavelet(ReadableWaveletData waveletData) {

    WaveletName wname = WaveletName.of(waveletData.getWaveId(), waveletData.getWaveletId());

    LOG.info("Indexing wavelet " + wname);

    for (ParticipantId p : waveletData.getParticipants()) {
      indexWavelet(waveletData, p);
    }

  }


  protected DBObject getOrCreateIndexedWave(WaveId waveId, ParticipantId participantId) {

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

  @SuppressWarnings("deprecation")
  protected void indexWavelet(ReadableWaveletData waveletData, ParticipantId participantId) {

    DBObject waveView = getOrCreateIndexedWave(waveletData.getWaveId(), participantId);
    WaveType waveType = WaveType.fromWaveId(waveletData.getWaveId());
    boolean isNew = waveView == null;

    if (isNew) {
      // Create an empty DBObject to place the wave view
      waveView = new BasicDBObject();
      waveView.put(FIELD_VIEW_FOR, participantId.getAddress());
      waveView.put(FIELD_WAVE_ID, (waveletData.getWaveId().serialise()));
      waveView.put(FIELD_TYPE, WaveType.serialize(waveType));

    } else {
      waveType = WaveType.deserialize((String) waveView.get(FIELD_TYPE));
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

        // conversation
        setConversationContentDBObject(waveView, waveletData, participantId);

      } else if (waveType.equals(WaveType.CHAT)) {

        // chat
        setConversationUserDataDBObject(waveView, waveletData, participantId);
      }

      // if it is a personal wavelet
    } else if (WaveExtendedModel.isUserDataWavelet(waveletData.getWaveletId())) {


      if (waveType.equals(WaveType.CONVERSATION)) {

        // If it's a user data wavelet, this only applies that user's index
        // view.
        if (participantId.equals(waveletData.getCreator()))
          setConversationUserDataDBObject(waveView, waveletData, participantId);

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

  private void setConversationContentDBObject(DBObject parent, ReadableWaveletData waveletData,
      ParticipantId participantId) {

    DBObject conversation = new BasicDBObject();
    String title = conversationUtils.getConversationTitle(waveletData);

    parent.put(FIELD_TITLE, title);
    conversation.put(FIELD_TITLE, title);


    parent.put(FIELD_CONTENT_CONVERSATION, conversation);
  }

  private void setConversationUserDataDBObject(DBObject parent,
      ReadableWaveletData waveletData, ParticipantId participantId) {

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
