package org.waveprotocol.wave.model.extended.type.chat;


import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.WaveletListener;

import java.util.HashMap;
import java.util.Map;

public class DocumentBasedChat implements ObservableChat {

  public static final String DOC_ID = "chat+main";

  private static final String CHAT_TAG = "chat";
  private static final String MESSAGE_TAG = "message";

  private static final String STATUS_TAG = "status";
  private static final String PARTICIPANT_TAG = "participant";


  private static final String CREATOR_ATTR = "c";
  private static final String TITLE_ATTR = "tl";

  private static final String HASH_ATTR = "h";
  private static final String TIMESTAMP_ATTR = "t";
  private static final String MESSAGE_ATTR = "m";

  private static final String PARTICIPANT_ID_ATTR = "id";
  private static final String PARTICIPANT_STATUS_ATTR = "s";

  /** An attribute storing the creator's id */
  private final BasicValue<String> creator;

  /** An attribute storing the title */
  private final BasicValue<String> title;

  /** A message store based on an doc-based element list */
  private final ObservableElementList<ChatMessage, ChatMessage.Initialiser> messages;

  private final ObservableBasicMap<String, ChatPresenceStatus> status;

  /** Listeners to this chat */
  private final CopyOnWriteSet<ObservableChat.Listener> listeners = CopyOnWriteSet.create();


  private final ObservableElementList.Listener<ChatMessage> listListener =
      new ObservableElementList.Listener<ChatMessage>() {

        @Override
        public void onValueRemoved(ChatMessage entry) {
          // this is a chat, so we don't expect remove messages
        }

        @Override
        public void onValueAdded(ChatMessage entry) {
          for (ObservableChat.Listener listener : listeners)
            listener.onMessageAdded(entry);
        }
      };


  private final ObservableBasicMap.Listener<String, ChatPresenceStatus> statusListener =
      new ObservableBasicMap.Listener<String, ChatPresenceStatus>() {

        @Override
        public void onEntrySet(String key, ChatPresenceStatus oldValue, ChatPresenceStatus newValue) {
          for (ObservableChat.Listener listener : listeners)
            listener.onParticipantStatusChanged(ParticipantId.ofUnsafe(key), newValue);
        }
      };


  private final WaveletListener waveletListener = new WaveletListener() {

    @Override
    public void onParticipantAdded(ObservableWavelet wavelet, ParticipantId participant) {
      for (ObservableChat.Listener listener : listeners)
        listener.onParticipantAdded(participant);
    }

    @Override
    public void onParticipantRemoved(ObservableWavelet wavelet, ParticipantId participant) {
      for (ObservableChat.Listener listener : listeners)
        listener.onParticipantRemoved(participant);
    }

    @Override
    public void onLastModifiedTimeChanged(ObservableWavelet wavelet, long oldTime, long newTime) {
      // Nothing to do
    }

    @Override
    public void onBlipAdded(ObservableWavelet wavelet, Blip blip) {
      // Nothing to do
    }

    @Override
    public void onBlipRemoved(ObservableWavelet wavelet, Blip blip) {
      // Nothing to do
    }

    @Override
    public void onBlipSubmitted(ObservableWavelet wavelet, Blip blip) {
      // Nothing to do
    }

    @Override
    public void onBlipTimestampModified(ObservableWavelet wavelet, Blip blip, long oldTime,
        long newTime) {
      // Nothing to do
    }

    @Override
    public void onBlipVersionModified(ObservableWavelet wavelet, Blip blip, Long oldVersion,
        Long newVersion) {
      // Nothing to do
    }

    @Override
    public void onBlipContributorAdded(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
      // Nothing to do
    }

    @Override
    public void onBlipContributorRemoved(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
      // Nothing to do
    }

    @Override
    public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {
      // Nothing to do
    }

    @Override
    public void onHashedVersionChanged(ObservableWavelet wavelet, HashedVersion oldHashedVersion,
        HashedVersion newHashedVersion) {
      // Nothing to do
    }

    @Override
    public void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip) {
      // Nothing to do
    }

  };



  static <E> Factory<E, ? extends ChatMessage, ChatMessage.Initialiser> messageFactory() {

    return new Factory<E, ChatMessage, ChatMessage.Initialiser>() {

      @Override
      public ChatMessage adapt(DocumentEventRouter<? super E, E, ?> router, E element) {

        String hash = router.getDocument().getAttribute(element, HASH_ATTR);
        String timestamp = router.getDocument().getAttribute(element, TIMESTAMP_ATTR);
        String creator = router.getDocument().getAttribute(element, CREATOR_ATTR);
        String text = router.getDocument().getAttribute(element, MESSAGE_ATTR);

        return new ChatMessage(hash, text, Long.valueOf(timestamp), ParticipantId.ofUnsafe(creator));
      }

      @Override
      public Initializer createInitializer(final ChatMessage.Initialiser initialState) {

        return new Initializer() {

          @Override
          public void initialize(Map<String, String> target) {

            target.put(HASH_ATTR, Serializer.STRING.toString(initialState.message.getHash()));
            target.put(CREATOR_ATTR,
                Serializer.STRING.toString(initialState.message.getCreator().getAddress()));
            target.put(TIMESTAMP_ATTR,
                Serializer.LONG.toString(initialState.message.getTimestamp()));
            target.put(MESSAGE_ATTR, Serializer.STRING.toString(initialState.message.getText()));
          }

        };

      }

    };
  }

  static <E> DocumentBasedChat create(DocumentEventRouter<? super E, E, ?> router,
 E chatContainer, E statusContainer) {

    return new DocumentBasedChat(DocumentBasedElementList.create(router, chatContainer,
        MESSAGE_TAG, DocumentBasedChat.<E> messageFactory()), DocumentBasedBasicMap.create(router,
        statusContainer, Serializer.STRING, new ChatPresenceStatus.StatusSerializer(),
        PARTICIPANT_TAG, PARTICIPANT_ID_ATTR, PARTICIPANT_STATUS_ATTR),
        DocumentBasedBasicValue.create(router, chatContainer, Serializer.STRING, CREATOR_ATTR),
        DocumentBasedBasicValue.create(router, chatContainer, Serializer.STRING, TITLE_ATTR));
  }


  /**
   * Creates a new DocumentBasedChat from an existing wavelet.
   * 
   * @param wavelet
   * @return
   */
  public static <E> DocumentBasedChat create(ObservableWavelet wavelet) {

    // Retrieve or create
    ObservableDocument doc = wavelet.getDocument(DOC_ID);

    Doc.E chatElement = DocHelper.getElementWithTagName(doc, CHAT_TAG);

    if (chatElement == null)
      chatElement =
          doc.createChildElement(doc.getDocumentElement(), CHAT_TAG, new HashMap<String, String>());

    Doc.E statusElement = DocHelper.getElementWithTagName(doc, STATUS_TAG);

    if (statusElement == null)
      statusElement =
          doc.createChildElement(doc.getDocumentElement(), STATUS_TAG,
              new HashMap<String, String>());

    DocumentBasedChat chat =
        create(DefaultDocumentEventRouter.create(doc), chatElement, statusElement);
    wavelet.addListener(chat.waveletListener);
    return chat;

  }

  /**
   *
   *
   * @param messages
   * @param creator
   */
  DocumentBasedChat(ObservableElementList<ChatMessage, ChatMessage.Initialiser> messages,
      ObservableBasicMap<String, ChatPresenceStatus> status, BasicValue<String> creator,
      BasicValue<String> title) {

    this.creator = creator;
    this.messages = messages;
    this.messages.addListener(listListener);

    this.status = status;
    this.status.addListener(statusListener);

    this.title = title;

  }


  @Override
  public void addMessage(ChatMessage message) {
    this.messages.add(message.getInitialiser());

  }

  @Override
  public int numMessages() {
    return this.messages.size();
  }

  @Override
  public Iterable<ChatMessage> getMessages() {
    return this.messages.getValues();
  }


  @Override
  public void addListener(Listener listener) {
    this.listeners.add(listener);

  }

  @Override
  public void removeListener(Listener listener) {
    this.listeners.remove(listener);
  }

  @Override
  public void setCreator(ParticipantId creator) {
    this.creator.set(creator.getAddress());
  }

  @Override
  public ParticipantId getCreator() {
    return ParticipantId.ofUnsafe(this.creator.get());
  }

  @Override
  public ChatMessage getMessage(int index) {
    return this.messages.get(index);
  }

  @Override
  public void setParticipantStatus(ParticipantId participant, ChatPresenceStatus status) {
    this.status.put(participant.getAddress(), status);
  }

  @Override
  public ChatPresenceStatus getParticipantStatus(ParticipantId participant) {
    return this.status.get(participant.getAddress());
  }

}
