package org.waveprotocol.wave.model.extended;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ReadOnlyWaveView;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.util.logging.Log;

/**
 * A utility class to manage Waves in our own way. Some of the code here is
 * taken from or inspired by other parts of the original project.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveConversationUtils {

  private static final Log LOG = Log.get(WaveConversationUtils.class);

  private static final int CONVERSATION_SNIPPET_LENGTH = 140;

  private final IdGenerator idGenerator;
  private final ConversationUtil conversationUtil;

  @Inject
  public WaveConversationUtils(IdGenerator idGenerator, ConversationUtil conversationUtil) {
    this.idGenerator = idGenerator;
    this.conversationUtil = conversationUtil;
  }

  /**
   * Builds an {@link ObservableConversationView} for the given wavelet. Note
   * that this can be expensive since the conversation is not garbage collected
   * until the wavelet is.
   *
   * @param wavelet The wavelet to return the conversation for, must be a valid
   *        conversation wavelet.
   * @throws IllegalArgumentException if the wavelet is not a valid conversation
   *         wavelet.
   */
  public ObservableConversationView buildConversationView(ObservableWavelet wavelet) {

    Preconditions.checkArgument(IdUtil.isConversationalId(wavelet.getId()),
        "Expected conversational wavelet, got " + wavelet.getId());

    Preconditions.checkArgument(WaveletBasedConversation.waveletHasConversation(wavelet),
        "Conversation can't be build on a wavelet " + wavelet.getId()
            + " without conversation structure");

    ReadOnlyWaveView wv = new ReadOnlyWaveView(wavelet.getWaveId());
    wv.addWavelet(wavelet);

    return WaveBasedConversationView.create(wv, idGenerator);
  }



  public String getWaveletConversationTitle(ObservableConversationView conversationWaveletWiew) {

    ObservableConversation rootConversation = conversationWaveletWiew.getRoot();

    ObservableConversationBlip firstBlip = null;
    if (rootConversation != null && rootConversation.getRootThread() != null
        && rootConversation.getRootThread().getFirstBlip() != null) {
      firstBlip = rootConversation.getRootThread().getFirstBlip();
    }
    String title;

    if (firstBlip != null) {
      Document firstBlipContents = firstBlip.getContent();
      title = TitleHelper.extractTitle(firstBlipContents).trim();
    } else {
      title = "";
    }

    return title;
  }

  public String getWaveletConversationSnippet(WaveletData rawWaveletData) {
    String snippet = Snippets.renderSnippet(rawWaveletData, CONVERSATION_SNIPPET_LENGTH).trim();
    return snippet;
  }


  public ObservableConversationView getConversationView(ObservableWaveletData waveletData) {
    ObservableConversationView conversationView = null;
    OpBasedWavelet wavelet = OpBasedWavelet.createReadOnly(waveletData);
    if (WaveletBasedConversation.waveletHasConversation(wavelet)) {
      conversationView = conversationUtil.buildConversation(wavelet);
    }
    return conversationView;
  }


  public String getConversationTitle(ReadableWaveletData conversationWavelet) {

    Preconditions.checkArgument(
        conversationWavelet.getDocumentIds().contains(IdConstants.MANIFEST_DOCUMENT_ID),
        "Expected conversational wavelet, got " + conversationWavelet.getWaveletId());

    String title = "";

    try {

      Document conversationDoc =
          conversationWavelet.getDocument(IdConstants.MANIFEST_DOCUMENT_ID).getContent()
              .getMutableDocument();

      N conversationNode = conversationDoc.getFirstChild(conversationDoc.getDocumentElement());
      N firstBlipNode = conversationDoc.getFirstChild(conversationNode);
      String firstBlipId =
          conversationDoc.getAttribute(conversationDoc.asElement(firstBlipNode), "id");

      Document firstBlipDoc =
          conversationWavelet.getDocument(firstBlipId).getContent().getMutableDocument();

      E firstLineElement = DocHelper.getElementWithTagName(firstBlipDoc, "line");
      E secondLineElement = DocHelper.getNextSiblingElement(firstBlipDoc, firstLineElement);

      title =
          DocHelper.getText(firstBlipDoc, Point.after(firstBlipDoc, firstLineElement),
              Point.before(firstBlipDoc, secondLineElement));

    } catch (Exception e) {
      LOG.severe("Error extracting conversation title from wavelet "
          + conversationWavelet.getWaveletId());
    }

    return title;
  }


}
