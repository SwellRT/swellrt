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

package org.waveprotocol.box.server.waveserver;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import javax.annotation.Nullable;

/**
 * Base implementation of {@link WaveIndexer}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public abstract class AbstractWaveIndexer implements WaveIndexer {

  protected final WaveMap waveMap;
  protected final WaveletProvider waveletProvider;

  public AbstractWaveIndexer(WaveMap waveMap, WaveletProvider waveletProvider) {
    this.waveletProvider = waveletProvider;
    this.waveMap = waveMap;
  }

  /*
   * TODO (Frank R.) move to TitleHelper. Currently, WaveletContainer is of
   * default visibility, which prevents the re-factoring.
   */
  // TODO (yurize) : the method is not used, should we remove it?
  @Nullable
  static String constructTitle(WaveId waveId, WaveletId waveletId, WaveMap waveMap,
      ConversationUtil conversationUtil) throws WaveletStateException {

    String title = null;

    WaveViewDataImpl wave = WaveViewDataImpl.create(waveId);

    WaveletName waveletname = WaveletName.of(waveId, waveletId);
    WaveletContainer waveletContainer = waveMap.getWavelet(waveletname);

    wave.addWavelet(waveletContainer.copyWaveletData());
    for (ObservableWaveletData waveletData : wave.getWavelets()) {
      OpBasedWavelet wavelet = OpBasedWavelet.createReadOnly(waveletData);
      if (WaveletBasedConversation.waveletHasConversation(wavelet)) {
        ObservableConversationView conversations = conversationUtil.buildConversation(wavelet);
        ObservableConversation root = conversations.getRoot();
        ObservableConversationBlip firstBlip = root.getRootThread().getFirstBlip();
        Document firstBlipContents = firstBlip.getContent();
        title = TitleHelper.extractTitle(firstBlipContents).trim();
        break;
      }
    }
    return title;
  }

  /**
   * Forces all waves to be loaded into memory and processes each wavelet.
   */
  @Override
  public synchronized void remakeIndex() throws WaveletStateException, WaveServerException {
    waveMap.loadAllWavelets();

    ExceptionalIterator<WaveId, WaveServerException> witr = waveletProvider.getWaveIds();
    while (witr.hasNext()) {
      WaveId waveId = witr.next();
      for (WaveletId waveletId : waveletProvider.getWaveletIds(waveId)) {
        WaveletName waveletName = WaveletName.of(waveId, waveletId);

        // Required to call this method to load the wavelet into memory.
        waveletProvider.getSnapshot(waveletName);
        processWavelet(waveletName);
      }
    }
  }

  /**
   * Provdes a hook to process the wavelet.
   *
   * @param waveletName
   */
  protected abstract void processWavelet(WaveletName waveletName);

  /**
   * Provides a hook to perform some logic after indexing was completed.
   */
  protected abstract void postIndexHook();

  protected WaveMap getWaveMap() {
    return waveMap;
  }

  protected WaveletProvider getWaveletProvider() {
    return waveletProvider;
  }
}
