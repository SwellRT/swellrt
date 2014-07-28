package org.waveprotocol.wave.model.extended.type;

import com.google.gwt.core.client.Duration;

import org.waveprotocol.wave.client.extended.WaveContentWrapper;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Doc.T;
import org.waveprotocol.wave.model.document.DocHandler;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.List;


/**
 * Wave document that stores a plain chat with following structure. (Based on @see
 * TagsDocument)
 * 
 * @author pablojan@gmail (Pablo Ojanguren)
 * 
 * @param <N>
 * @param <E>
 * @param <T>
 */
@Deprecated
public class ChatContent implements DocHandler, ObservableConversation.Listener {


  public static String DOC_ID = "chatdoc";

  /**
   * A listener interface to receive changes on the chat
   *
   */
  public interface Listener {

    void onAdd(String chatLine);

    void onAddParticipant(ParticipantId participantId);

    void onRemoveParticipant(ParticipantId participantId);
  }

  /** The wavelet doc supporting the data */
  private final ObservableDocument doc;

  /** We still need the wave for participant mgmt */
  private final WaveContentWrapper waveWrapper;

  /** List of listeners of this doc */
  private final List<Listener> listeners;

  /** Who is writing locally this doc */
  private final ParticipantId contributor;



  private ChatContent(WaveContentWrapper waveWrapper, ObservableDocument document,
      ParticipantId contributor) {
    this.doc = document;
    this.waveWrapper = waveWrapper;
    this.contributor = contributor;
    this.listeners = new ArrayList<Listener>();

  }

  public static ChatContent create(WaveContentWrapper waveWrapper, ParticipantId contributor) {


    // ObservableDocument rawDocument =
    // waveWrapper.createDocumentInRoot(DOC_ID);
    // ChatContent chatBackend =
    // new ChatContent(waveWrapper, rawDocument, contributor);
    //
    // rawDocument.addListener(chatBackend);
    // waveWrapper.addConversationListener(chatBackend);
    //
    // return chatBackend;

    return null;
  }

  /**
   * Add a listener for changes to the document
   *
   * @param listener A listener interested in Chat changes
   */
  public void addListener(Listener listener) {
    listeners.add(listener);
  }


  public List<String> getReverseLines(int from, int to) {
    return null;
  }

  /**
   * Adds a new chat line at the end
   *
   * @param lineText Text to be added
   */
  public void addChatLine(String lineText) {
    XmlStringBuilder xml =
        createLineTag(this.contributor.toString() + "|"
            + Double.toString(Duration.currentTimeMillis()) + "|" + lineText);
    doc.appendXml(xml);
  }


  private XmlStringBuilder createLineTag(String lineText) {
    return XmlStringBuilder.createText(lineText).wrap("line");
  }


  public void addParticipant(ParticipantId participantId) {

    // Participant mgmt delegated to the Wave Wrapper
    // this.waveWrapper.addParticipant(participantId);
  }

  //
  // Manage Doc Events
  //

  @Override
  public void onDocumentEvents(DocumentHandler.EventBundle<N, E, T> event) {

    for (E e : event.getInsertedElements()) {

      String newLine = doc.getData(doc.asText(doc.getFirstChild(e)));

      // No need to filter local messages, this will update the view properly

      for (Listener listener : this.listeners) {
        listener.onAdd(newLine);
      }

    }
  }


  //
  // Manage Wavelet Events
  //


  @Override
  public void onParticipantAdded(ParticipantId participant) {
    for (Listener listener : this.listeners) {
      listener.onAddParticipant(participant);
    }
  }

  @Override
  public void onParticipantRemoved(ParticipantId participant) {
    for (Listener listener : this.listeners) {
      listener.onRemoveParticipant(participant);
    }
  }

  @Override
  public void onBlipAdded(ObservableConversationBlip blip) {
    // N/A

  }

  @Override
  public void onBlipDeleted(ObservableConversationBlip blip) {
    // N/A

  }

  @Override
  public void onThreadAdded(ObservableConversationThread thread) {
    // N/A

  }

  @Override
  public void onInlineThreadAdded(ObservableConversationThread thread, int location) {
    // N/A

  }

  @Override
  public void onThreadDeleted(ObservableConversationThread thread) {
    // N/A

  }

  @Override
  public void onBlipContributorAdded(ObservableConversationBlip blip, ParticipantId contributor) {
    // N/A

  }

  @Override
  public void onBlipContributorRemoved(ObservableConversationBlip blip, ParticipantId contributor) {
    // N/A

  }

  @Override
  public void onBlipSumbitted(ObservableConversationBlip blip) {
    // N/A

  }

  @Override
  public void onBlipTimestampChanged(ObservableConversationBlip blip, long oldTimestamp,
      long newTimestamp) {
    // N/A

  }


}
