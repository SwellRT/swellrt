package org.waveprotocol.wave.model.extended;

import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * Utility class to work with the extended wave model
 * 
 * The extended wave model is intended to be backward compatible with original
 * model and protocol. It adds the following characteristics:
 * 
 * A wave has a content-wavelet and one user-wavelet per participant. A
 * content-wavelet has one type (conversation, chat or doc). A content-wavelet
 * has multiple contents of different types (blip, doc, chat, comment). e.g. A
 * doc wave can have a document itself, an associated chat and some user's
 * comments/discussions
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public class WaveExtendedModel {


  // TODO(pablojan) duplicated in DocumentBasedChat, resolve conflict
  public static final String CHAT_DOC_ID = "c/chat";

  public static final String CONTENT_WAVELET_CONVERSATION_PREFIX =
      IdUtil.CONVERSATION_WAVELET_PREFIX;
  public static final String CONTENT_WAVELET_DOCUMENT_PREFIX = "doc";
  public static final String CONTENT_WAVELET_CHAT_PREFIX = "chat";
  public static final String CONTENT_WAVELET_APP_PREFIX = "app";

  public static final String CONTENT_WAVELET_ROOT = "root";


  public static boolean isContentWavelet(WaveletId waveletId) {

    String typeToken = IdUtil.getInitialToken(waveletId.getId());

    if (IdUtil.isConversationalId(waveletId) || typeToken.equals(CONTENT_WAVELET_DOCUMENT_PREFIX)
        || typeToken.equals(CONTENT_WAVELET_CHAT_PREFIX)
        || typeToken.equals(CONTENT_WAVELET_APP_PREFIX)) return true;

    return false;
  }

  public static boolean isUserDataWavelet(WaveletId waveletId) {
    return IdUtil.isUserDataWavelet(waveletId);
  }

}
