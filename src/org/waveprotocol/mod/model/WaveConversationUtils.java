package org.waveprotocol.mod.model;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.supplement.SupplementImpl;
import org.waveprotocol.wave.model.supplement.WaveletBasedSupplement;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ReadOnlyWaveView;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.util.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
  private static final String TITLE_NOT_AVAILABLE_MSG = "(No title)";

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




  public String getWaveletConversationSnippet(ReadableWaveletData rawWaveletData) {
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


  public String getConversationFirstLineText(ReadableWaveletData conversationWavelet) {

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

  public String getWaveletConversationTitle(ReadableWaveletData conversationWavelet) {

    Document conversationDoc =
        conversationWavelet.getDocument(IdConstants.MANIFEST_DOCUMENT_ID).getContent()
            .getMutableDocument();

    if (conversationDoc == null) return TITLE_NOT_AVAILABLE_MSG;

    N conversationNode = conversationDoc.getFirstChild(conversationDoc.getDocumentElement());

    if (conversationNode == null) return TITLE_NOT_AVAILABLE_MSG;

    N firstBlipNode = conversationDoc.getFirstChild(conversationNode);

    if (firstBlipNode == null) return TITLE_NOT_AVAILABLE_MSG;

    String firstBlipId =
        conversationDoc.getAttribute(conversationDoc.asElement(firstBlipNode), "id");

    if (firstBlipId == null || firstBlipId.isEmpty()) return TITLE_NOT_AVAILABLE_MSG;

    Document firstBlipDoc =
        conversationWavelet.getDocument(firstBlipId).getContent().getMutableDocument();

    if (firstBlipDoc == null) return TITLE_NOT_AVAILABLE_MSG;


   String title = TitleHelper.extractTitle(firstBlipDoc);

   if (title.isEmpty())
 title = getConversationFirstLineText(conversationWavelet);

    if (title.isEmpty()) title = TITLE_NOT_AVAILABLE_MSG;

    return title;
  }

  public Map<String, Long> getConversationBlips(ReadableWaveletData conversationWavelet) {

    Preconditions.checkNotNull(conversationWavelet);
    Preconditions.checkArgument(IdUtil.isConversationalId(conversationWavelet.getWaveletId()));

    HashMap<String, Long> blipsMap = new HashMap<String, Long>();

    if (!conversationWavelet.getDocumentIds().contains(
        IdUtil.MANIFEST_DOCUMENT_ID))
      return blipsMap;


    ReadableBlipData manifestBlip = conversationWavelet.getDocument(IdUtil.MANIFEST_DOCUMENT_ID);
    Document manifestDoc = manifestBlip.getContent().getMutableDocument();
    List<Doc.E> blipElements = ExtendedDocHelper.getAllElementsByTagName("blip", manifestDoc);

    for (Doc.E element : blipElements) {
      String blipId = manifestDoc.getAttribute(element, "id");
      Long blipVersion = null;
      if (conversationWavelet.getDocumentIds().contains(blipId)) {
        blipVersion = conversationWavelet.getDocument(blipId).getLastModifiedVersion();
        blipsMap.put(blipId, blipVersion);
      }
    }

    return blipsMap;
  }

  public Map<String, Long> getUserDataBlips(ReadableWaveletData userDataWavelet) {

    Preconditions.checkNotNull(userDataWavelet);
    Preconditions.checkArgument(IdUtil.isUserDataWavelet(userDataWavelet.getWaveletId()));


    /**
     * We suppose to have only one conversation (conv+root), so we are counting
     * all the blip tags in the documento.
     */

    HashMap<String, Long> blipsMap = new HashMap<String, Long>();

    if (!userDataWavelet.getDocumentIds().contains(WaveletBasedSupplement.READSTATE_DOCUMENT))
      return blipsMap;


    ReadableBlipData manifestBlip =
        userDataWavelet.getDocument(WaveletBasedSupplement.READSTATE_DOCUMENT);
    Document manifestDoc = manifestBlip.getContent().getMutableDocument();
    List<Doc.E> blipElements = ExtendedDocHelper.getAllElementsByTagName("blip", manifestDoc);

    for (Doc.E element : blipElements) {
      String blipId = manifestDoc.getAttribute(element, "i");
      String blipVersion = manifestDoc.getAttribute(element, "v");
      try {
        blipsMap.put(blipId, Long.valueOf(blipVersion));
      } catch (NumberFormatException e) {

      }

    }

    return blipsMap;
  }


  /**
   * Calculates the number of blips not read yet by an user. It's a simplistic
   * implementation based on {@link SupplementImpl.isBlipUnread}. It doesn't
   * take care of wavelet-override version.
   *
   * @param contentBlips
   * @param userBlips
   * @return
   */
  public int getNotReadBlips(Map<String, Long> contentBlips, Map<String, Long> userBlips) {

    int nonReadCount = contentBlips.size();
    for (Entry<String, Long> docBlip : contentBlips.entrySet()) {

      if (userBlips.containsKey(docBlip.getKey())) {
        Long blipUserVersion = userBlips.get(docBlip.getKey());
        if (blipUserVersion != null && blipUserVersion >= docBlip.getValue()) nonReadCount--;
      }

    }

    return nonReadCount;
  }

}
