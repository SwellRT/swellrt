/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.waveprotocol.wave.client.wavepanel.impl.focus;

import org.waveprotocol.wave.client.wavepanel.impl.focus.FocusFramePresenter.FocusOrder;
import org.waveprotocol.wave.client.wavepanel.impl.reader.Reader;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Collections;
import java.util.Map;

/**
 * Selects the blip that should should receive the focus.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class FocusBlipSelector {

  /** The reference to the current wave. */
  private final WaveRef waveRef;

  /** The conversation. */
  private final ConversationView wave;

  /** The model with views. */
  private final ModelAsViewProvider views;

  /** The blip ordering. */
  private final  ViewTraverser traverser;

  /** The ordering for focus frame movement. */
  private final Reader reader;

  /**
   * Creates a {@link FocusBlipSelector}.
   *
   * @param waveRef the reference to the current wave that includes the blip id
   *        in case the wave was loaded due to a click on a link to this wave.
   * @param wave the he conversation.
   * @param views the model with views.
   * @param traverser the blip ordering.
   * @param reader the ordering of focus frame movement.
   * @return the focus blip selector.
   */
  public static FocusBlipSelector create(WaveRef waveRef, ConversationView wave,
      ModelAsViewProvider views, Reader reader, ViewTraverser traverser) {
    return new FocusBlipSelector(waveRef, wave, views, reader, traverser);
  }

  FocusBlipSelector(WaveRef waveRef, ConversationView wave,
      ModelAsViewProvider models, Reader reader, ViewTraverser traverser) {
    this.waveRef = waveRef;
    this.wave = wave;
    this.views = models;
    this.reader = reader;
    this.traverser = traverser;
  }

  /**
   * @return the blip that should receive the focus or {@code null} if it was
   *         impossible to compute it. The strategy for blip selection is like
   *         follows:
   *         <ul>
   *         <li>Try to select the oldest unread blip using {@link FocusOrder}.</li>
   *         <li>If all blips are already read, then select the most recently
   *         modified blip.</li>
   *         </ul>
   */
  public BlipView select() {
    // Determine if waveRef has a documentId in it - if so, the referenced blip
    // should receive the focus on wave load.
    // First find conversation
    Conversation conversation;
    if (waveRef.hasWaveletId()) {
      String id = ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveRef.getWaveletId());
      conversation = wave.getConversation(id);
    } else {
      // Unspecified wavelet means root.
      conversation = wave.getRoot();
    }
    if (conversation == null) {
      return null;
    } else {
      ConversationBlip blip = null;
      // If there's blip reference then focus on that blip.
      String documentId = waveRef.getDocumentId();
      // Find selected blip.
      if (documentId != null) {
        blip = wave.getRoot().getBlip(documentId);
        if (blip != null) {
          return views.getBlipView(blip);
        } else {
          return null;
        }
      } else {
        blip = wave.getRoot().getRootThread().getFirstBlip();
        BlipView rootBlipUi = views.getBlipView(blip);
        if (rootBlipUi == null) {
          return null;
        }
        if (reader != null) {
          if (!reader.isRead(rootBlipUi)) {
            // Return the root blip since it is unread.
            return rootBlipUi;
          }
          // If no blip was referenced, then try to find the next blip starting
          // from the root according to the order.
          BlipView tempUi = reader.getNext(rootBlipUi);
          if (tempUi != null && !tempUi.getId().equals(rootBlipUi.getId())) {
            return tempUi;
          } else {
            return findMostRecentlyModified(rootBlipUi);
          }
        } else {
          return findMostRecentlyModified(rootBlipUi);
        }
      }
    }
  }

  private BlipView findMostRecentlyModified(BlipView start) {
    BlipView blipUi = start;
    Map<Long, BlipView> blips = CollectionUtils.newHashMap();
    while (blipUi != null) {
      ConversationBlip blip = views.getBlip(blipUi);
      blips.put(blip.getLastModifiedTime() , blipUi);
      blipUi = traverser.getNext(blipUi);
    }
    long lmt = Collections.max(blips.keySet());
    return blips.get(lmt);
  }
}
