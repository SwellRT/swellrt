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

package org.waveprotocol.box.server.rpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.MessageLite;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationQueue;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;
import com.google.wave.api.data.converter.EventDataConverterManager;

import org.waveprotocol.box.search.SearchProto.SearchRequest;
import org.waveprotocol.box.search.SearchProto.SearchResponse;
import org.waveprotocol.box.search.SearchProto.SearchResponse.Builder;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.rpc.ProtoSerializer.SerializationException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.webclient.search.SearchService;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet to provide search functionality by using Data API. Typically will
 * be hosted on /search.
 *
 * Valid request format is: GET /search/?query=in:inbox&index=0&numResults=50.
 * The format of the returned information is the protobuf-JSON format used by
 * the websocket interface.
 *
 * @author vega113@gmail.com (Yuri Z.)
 */
@SuppressWarnings("serial")
@Singleton
public final class SearchServlet extends HttpServlet {

  private static final Log LOG = Log.get(SearchServlet.class);

  public static class SearchResponseUtils {

    /**
     * Extracts search query params from request.
     *
     * @param req the request.
     * @param response the response.
     * @return the SearchRequest with query data.
     */
    public static SearchRequest parseSearchRequest(HttpServletRequest req,
        HttpServletResponse response) {

      String query = req.getParameter("query");
      String index = req.getParameter("index");
      String numResults = req.getParameter("numResults");
      SearchRequest searchRequest =
          SearchRequest.newBuilder().setQuery(query).setIndex(Integer.parseInt(index))
              .setNumResults(Integer.parseInt(numResults)).build();
      return searchRequest;
    }

    /**
     * Constructs SearchResponse which is a protobuf generated class from the
     * output of Data API search service. SearchResponse contains the same
     * information as searchResult.
     *
     * @param searchResult the search results with digests.
     * @return SearchResponse
     */
    public static SearchResponse serializeSearchResult(SearchResult searchResult, int total) {
      Builder searchBuilder = SearchResponse.newBuilder();
      searchBuilder.setQuery(searchResult.getQuery()).setTotalResults(total);
      for (SearchResult.Digest searchResultDigest : searchResult.getDigests()) {
        SearchResponse.Digest digest = serializeDigest(searchResultDigest);
        searchBuilder.addDigests(digest);
      }
      SearchResponse searchResponse = searchBuilder.build();
      return searchResponse;
    }

    /**
     * Copies data from {@link Digest} into {@link SearchResponse.Digest}.
     */
    private static SearchResponse.Digest serializeDigest(Digest searchResultDigest) {
      SearchResponse.Digest.Builder digestBuilder = SearchResponse.Digest.newBuilder();
      digestBuilder.setBlipCount(searchResultDigest.getBlipCount());
      digestBuilder.setLastModified(searchResultDigest.getLastModified());
      digestBuilder.setSnippet(searchResultDigest.getSnippet());
      digestBuilder.setTitle(searchResultDigest.getTitle());
      digestBuilder.setUnreadCount(searchResultDigest.getUnreadCount());
      digestBuilder.setWaveId(searchResultDigest.getWaveId());
      List<String> participants = searchResultDigest.getParticipants();
      if (participants.isEmpty()) {
        // This shouldn't be possible.
        digestBuilder.setAuthor("nobody@example.com");
      } else {
        digestBuilder.setAuthor(participants.get(0));
        for (int i = 1; i < participants.size(); i++) {
          digestBuilder.addParticipants(participants.get(i));
        }
      }
      SearchResponse.Digest digest = digestBuilder.build();
      return digest;
    }
  }

  private final ConversationUtil conversationUtil;
  private final EventDataConverterManager converterManager;
  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;
  private final OperationServiceRegistry operationRegistry;
  private final ProtoSerializer serializer;


  @Inject
  public SearchServlet(SessionManager sessionManager, EventDataConverterManager converterManager,
      @Named("DataApiRegistry") OperationServiceRegistry operationRegistry,
      WaveletProvider waveletProvider, ConversationUtil conversationUtil, ProtoSerializer serializer) {
    this.converterManager = converterManager;
    this.waveletProvider = waveletProvider;
    this.conversationUtil = conversationUtil;
    this.sessionManager = sessionManager;
    this.operationRegistry = operationRegistry;
    this.serializer = serializer;
  }


  /**
   * Creates HTTP response to the search query. Main entrypoint for this class.
   */
  @Override
  @VisibleForTesting
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
    ParticipantId user = sessionManager.getLoggedInUser(req.getSession(false));
    if (user == null) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    SearchRequest searchRequest = SearchResponseUtils.parseSearchRequest(req, response);
    SearchResponse searchResponse = performSearch(searchRequest, user);
    serializeObjectToServlet(searchResponse, response);
  }

  /**
   * Performs search using Data API.
   */
  private SearchResponse performSearch(SearchRequest searchRequest, ParticipantId user) {
    OperationQueue opQueue = new OperationQueue();
    opQueue.search(searchRequest.getQuery(), searchRequest.getIndex(),
        searchRequest.getNumResults());
    OperationContextImpl context =
        new OperationContextImpl(waveletProvider,
            converterManager.getEventDataConverter(ProtocolVersion.DEFAULT), conversationUtil);
    LOG.fine(
        "Performing query: " + searchRequest.getQuery() + " [" + searchRequest.getIndex() + ", "
            + (searchRequest.getIndex() + searchRequest.getNumResults()) + "]");
    OperationRequest operationRequest = opQueue.getPendingOperations().get(0);
    String opId = operationRequest.getId();
    OperationUtil.executeOperation(operationRequest, operationRegistry, context, user);
    JsonRpcResponse jsonRpcResponse = context.getResponses().get(opId);
    SearchResult searchResult =
        (SearchResult) jsonRpcResponse.getData().get(ParamsProperty.SEARCH_RESULTS);

    // The Data API does not return the total size of the search result, even
    // though the searcher knows it. The only approximate knowledge that can be
    // gleaned from the Data API is whether there are more search results beyond
    // those returned. If the searcher returns as many (or more) results as
    // requested, then assume that more results exist, but the total is unknown.
    // Otherwise, the total has been reached.
    int totalGuess;
    if (searchResult.getNumResults() >= searchRequest.getNumResults()) {
      totalGuess = SearchService.UNKNOWN_SIZE;
    } else {
      totalGuess = searchRequest.getIndex() + searchResult.getNumResults();
    }
    LOG.fine("Results: " + searchResult.getNumResults() + ", total: " + totalGuess);
    return SearchResponseUtils.serializeSearchResult(searchResult, totalGuess);
  }

  /**
   * Writes the json with search results to Response.
   */
  private void serializeObjectToServlet(MessageLite message, HttpServletResponse resp)
      throws IOException {
    if (message == null) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    } else {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("application/json");
      // This is to make sure the fetched data is fresh - since the w3c spec
      // is rarely respected.
      resp.setHeader("Cache-Control", "no-store");
      try {
        resp.getWriter().append(serializer.toJson(message).toString());
      } catch (SerializationException e) {
        throw new IOException(e);
      }
    }
  }
}
