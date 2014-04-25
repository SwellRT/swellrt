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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.HttpStatus;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * @author Frank R. <renfeng.cn@gmail.com>
 */
@Singleton
public class SolrWaveIndexerImpl extends AbstractWaveIndexer implements WaveBus.Subscriber,
PerUserWaveViewBus.Listener {

  private static final Log LOG = Log.get(SolrWaveIndexerImpl.class);

  // TODO (Yuri Z.): Inject executor.
  private static final Executor executor = Executors.newSingleThreadExecutor();

  /*
   * (regression alert) for getting the title of a wave
   */
  // private final ConversationUtil conversationUtil;

  private final ReadableWaveletDataProvider waveletDataProvider;


  @Inject
  public SolrWaveIndexerImpl(WaveMap waveMap, WaveletProvider waveletProvider,
      ReadableWaveletDataProvider waveletDataProvider, ConversationUtil conversationUtil,
      WaveletNotificationDispatcher notificationDispatcher) {

    super(waveMap, waveletProvider);

    this.waveletDataProvider = waveletDataProvider;

    /*
     * (regression alert) for getting the title of a wave
     */
    // this.conversationUtil = conversationUtil;

    notificationDispatcher.subscribe(this);

    return;
  }

  @Override
  public ListenableFuture<Void> onParticipantAdded(final WaveletName waveletName,
      ParticipantId participant) {
    /*
     * ignored. See waveletCommitted(WaveletName, HashedVersion)
     */
    return null;
  }

  @Override
  public ListenableFuture<Void> onParticipantRemoved(final WaveletName waveletName,
      ParticipantId participant) {
    /*
     * ignored. See waveletCommitted(WaveletName, HashedVersion)
     */
    return null;
  }

  @Override
  public ListenableFuture<Void> onWaveInit(final WaveletName waveletName) {

    ListenableFutureTask<Void> task = ListenableFutureTask.create(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        ReadableWaveletData waveletData;
        try {
          waveletData = waveletDataProvider.getReadableWaveletData(waveletName);
          updateIndex(waveletData);
        } catch (WaveServerException e) {
          LOG.log(Level.SEVERE, "Failed to initialize index for " + waveletName, e);
          throw e;
        }
        return null;
      }
    });
    executor.execute(task);
    return task;
  }

  @Override
  protected void processWavelet(WaveletName waveletName) {
    onWaveInit(waveletName);
  }

  @Override
  protected void postIndexHook() {
    try {
      getWaveMap().unloadAllWavelets();
    } catch (WaveletStateException e) {
      throw new IndexException("Problem encountered while cleaning up", e);
    }
  }

  private void updateIndex(ReadableWaveletData wavelet) throws IndexException {

    Preconditions.checkNotNull(wavelet);
    if (!IdUtil.isConversationalId(wavelet.getWaveletId())) {
      return;
    }

    PostMethod postMethod =
        new PostMethod(SolrSearchProviderImpl.SOLR_BASE_URL + "/update/json?commit=true");
    try {
      JsonArray docsJson = new JsonArray();

      String waveId = wavelet.getWaveId().serialise();
      String waveletId = wavelet.getWaveletId().serialise();
      String modified = Long.toString(wavelet.getLastModifiedTime());
      String creator = wavelet.getCreator().getAddress();

      for (String docName : wavelet.getDocumentIds()) {
        ReadableBlipData document = wavelet.getDocument(docName);

        if (!IdUtil.isBlipId(docName)) {
          continue;
        }

        Iterable<DocInitialization> ops = Lists.newArrayList(
            document.getContent().asOperation());
        String text = Snippets.collateTextForOps(ops, new Function<StringBuilder, Void>() {

          @Override
          public Void apply(StringBuilder resultBuilder) {
            resultBuilder.append("\n");
            return null;
          }

        });

        JsonArray participantsJson = new JsonArray();
        for (ParticipantId participant : wavelet.getParticipants()) {
          String participantAddress = participant.toString();
          participantsJson.add(new JsonPrimitive(participantAddress));
        }

        String id =
            JavaWaverefEncoder.encodeToUriPathSegment(WaveRef.of(wavelet.getWaveId(),
                wavelet.getWaveletId(), docName));

        JsonObject docJson = new JsonObject();
        docJson.addProperty(SolrSearchProviderImpl.ID, id);
        docJson.addProperty(SolrSearchProviderImpl.WAVE_ID, waveId);
        docJson.addProperty(SolrSearchProviderImpl.WAVELET_ID, waveletId);
        docJson.addProperty(SolrSearchProviderImpl.DOC_NAME, docName);
        docJson.addProperty(SolrSearchProviderImpl.LMT, modified);
        docJson.add(SolrSearchProviderImpl.WITH, participantsJson);
        docJson.add(SolrSearchProviderImpl.WITH_FUZZY, participantsJson);
        docJson.addProperty(SolrSearchProviderImpl.CREATOR, creator);
        docJson.addProperty(SolrSearchProviderImpl.TEXT, text);
        docJson.addProperty(SolrSearchProviderImpl.IN, "inbox");

        docsJson.add(docJson);
      }

      RequestEntity requestEntity =
          new StringRequestEntity(docsJson.toString(), "application/json", "UTF-8");
      postMethod.setRequestEntity(requestEntity);

      HttpClient httpClient = new HttpClient();
      int statusCode = httpClient.executeMethod(postMethod);
      if (statusCode != HttpStatus.SC_OK) {
        throw new IndexException(waveId);
      }

    } catch (IOException e) {
      throw new IndexException(String.valueOf(wavelet.getWaveletId()), e);
    } finally {
      postMethod.releaseConnection();
    }

    return;
  }

  /*
   * TODO (Frank R.) move to TitleHelper. Currently, WaveletContainer is of
   * default visibility, which prevents the refactoring.
   */
  public static String getTitle(WaveId waveId, WaveletId waveletId, WaveMap waveMap,
      ConversationUtil conversationUtil) {

    String title = null;

    WaveViewDataImpl wave = WaveViewDataImpl.create(waveId);

    WaveletContainer waveletContainer = null;
    WaveletName waveletname = WaveletName.of(waveId, waveletId);

    /*-
     * copied from
     *
    org.waveprotocol.box.server.waveserver.SimpleSearchProviderImpl.filterWavesViewBySearchCriteria(Function<ReadableWaveletData,
    Boolean>, Multimap<WaveId, WaveletId>)
     */
    // TODO (alown): Find some way to use isLocalWavelet to do this properly!
    try {
      if (LOG.isFineLoggable()) {
        LOG.fine("Trying as a remote wavelet");
      }
      waveletContainer = waveMap.getRemoteWavelet(waveletname);
    } catch (WaveletStateException e) {
      LOG.severe(String.format("Failed to get remote wavelet %s", waveletname.toString()), e);
    } catch (NullPointerException e) {
      // This is a fairly normal case of it being a local-only wave.
      // Yet this only seems to appear in the test suite.
      // Continuing is completely harmless here.
      LOG.info(String.format("%s is definitely not a remote wavelet. (Null key)",
          waveletname.toString()), e);
    }

    if (waveletContainer == null) {
      try {
        if (LOG.isFineLoggable()) {
          LOG.fine("Trying as a local wavelet");
        }
        waveletContainer = waveMap.getLocalWavelet(waveletname);
      } catch (WaveletStateException e) {
        LOG.severe(String.format("Failed to get local wavelet %s", waveletname.toString()), e);
      }
    }

    try {
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
    } catch (WaveletStateException e) {
      LOG.warning("Failed to access wavelet " + waveletContainer.getWaveletName(), e);
    }

    return title;
  }

  @Override
  public void waveletUpdate(final ReadableWaveletData wavelet, DeltaSequence deltas) {
    /*
     * (regression alert) commented out for optimization, see
     * waveletCommitted(WaveletName, HashedVersion)
     */
    // updateIndex(wavelet);
  }

  @Override
  public void waveletCommitted(final WaveletName waveletName, final HashedVersion version) {

    Preconditions.checkNotNull(waveletName);

    /*
     * (regression alert) don't update on current thread to prevent lock error
     */
    ListenableFutureTask<Void> task = ListenableFutureTask.create(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        ReadableWaveletData waveletData;
        try {
          waveletData = waveletDataProvider.getReadableWaveletData(waveletName);
          LOG.fine("commit " + version + " " + waveletData.getVersion());
          if (waveletData.getVersion() == version.getVersion()) {
            updateIndex(waveletData);
          }
        } catch (WaveServerException e) {
          LOG.log(Level.SEVERE, "Failed to update index for " + waveletName, e);
          throw e;
        }
        return null;
      }
    });
    executor.execute(task);

    return;
  }

  @Override
  public synchronized void remakeIndex() throws WaveletStateException, WaveServerException {

    /*-
     * to fully rebuild the index, need to delete everything first
     * the <query> tag should contain the value of
     * org.waveprotocol.box.server.waveserver.SolrSearchProviderImpl.Q
     *
     * http://localhost:8983/solr/update?stream.body=<delete><query>waveId_s:[*%20TO%20*]%20AND%20waveletId_s:[*%20TO%20*]%20AND%20docName_s:[*%20TO%20*]%20AND%20lmt_l:[*%20TO%20*]%20AND%20with_ss:[*%20TO%20*]%20AND%20with_txt:[*%20TO%20*]%20AND%20creator_t:[*%20TO%20*]</query></delete>
     * http://localhost:8983/solr/update?stream.body=<commit/>
     *
     * see
     * http://wiki.apache.org/solr/FAQ#How_can_I_delete_all_documents_from_my_index.3F
     */

    GetMethod getMethod = new GetMethod();
    try {
      getMethod
      .setURI(new URI(SolrSearchProviderImpl.SOLR_BASE_URL + "/update?wt=json"
          + "&stream.body=<delete><query>" + SolrSearchProviderImpl.Q + "</query></delete>",
          false));

      HttpClient httpClient = new HttpClient();
      int statusCode = httpClient.executeMethod(getMethod);
      if (statusCode == HttpStatus.SC_OK) {
        getMethod.setURI(new URI(SolrSearchProviderImpl.SOLR_BASE_URL + "/update?wt=json"
            + "&stream.body=<commit/>", false));

        httpClient = new HttpClient();
        statusCode = httpClient.executeMethod(getMethod);
        if (statusCode != HttpStatus.SC_OK) {
          LOG.warning("failed to clean solr index");
        }
      } else {
        LOG.warning("failed to clean solr index");
      }
    } catch (Exception e) {
      LOG.warning("failed to clean solr index", e);
    } finally {
      getMethod.releaseConnection();
    }

    super.remakeIndex();

    return;
  }
}
