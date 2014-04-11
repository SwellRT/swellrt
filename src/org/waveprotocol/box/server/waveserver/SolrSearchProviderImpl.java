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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.wave.api.SearchResult;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Search provider that offers full text search
 *
 * @author Frank R. <renfeng.cn@gmail.com>
 */
public class SolrSearchProviderImpl extends SimpleSearchProviderImpl implements SearchProvider {

  private static final Log LOG = Log.get(SolrSearchProviderImpl.class);

  private static final String WORD_START = "(\\b|^)";
  private static final Pattern IN_PATTERN = Pattern.compile("\\bin:\\S*");

  public static final int ROWS = 10;

  public static final String ID = "id";
  public static final String WAVE_ID = "waveId_s";
  public static final String WAVELET_ID = "waveletId_s";
  public static final String DOC_NAME = "docName_s";
  public static final String LMT = "lmt_l";
  public static final String WITH = "with_ss";
  public static final String WITH_FUZZY = "with_txt";
  public static final String CREATOR = "creator_t";
  public static final String TEXT = "text_t";
  public static final String IN = "in_ss";

  /*
   * TODO (Frank R.) make it configurable
   */
  public static final String SOLR_BASE_URL = "http://localhost:8983/solr";

  /*-
   * http://wiki.apache.org/solr/CommonQueryParameters#q
   *
   * (regression alert) the commented enables empty wave to be listed in search results
   */
  public static final String Q = WAVE_ID + ":[* TO *]" //
      + " AND " + WAVELET_ID + ":[* TO *]" //
      + " AND " + DOC_NAME + ":[* TO *]" //
      + " AND " + LMT + ":[* TO *]" //
      + " AND " + WITH + ":[* TO *]" //
      + " AND " + WITH_FUZZY + ":[* TO *]" //
      + " AND " + CREATOR + ":[* TO *]" //
      /* + " AND " + TEXT + ":[* TO *]" */;

  /*-
   * XXX (Frank R.) (experimental and disabled) edismax query parser
   *
   * mm (Minimum 'Should' Match)
   * http://wiki.apache.org/solr/ExtendedDisMax#mm_.28Minimum_.27Should.27_Match.29
   *
   * !edismax ignores "q.op=AND", see
   *
   * ExtendedDismaxQParser (edismax) does not obey q.op for queries with operators
   * https://issues.apache.org/jira/browse/SOLR-3741
   *
   * ExtendedDismaxQParser (edismax) does not obey q.op for parenthesized sub-queries
   * https://issues.apache.org/jira/browse/SOLR-3740
   */
  // public static final String FILTER_QUERY_PREFIX = "{!edismax q.op=AND df=" +
  // TEXT + "}" //
  // + WITH + ":";
  private static final String FILTER_QUERY_PREFIX = "{!lucene q.op=AND df=" + TEXT + "}" //
      + WITH + ":";


  public static Function<ReadableWaveletData, Boolean> matchesFunction =
      new Function<ReadableWaveletData, Boolean>() {

        @Override
        public Boolean apply(ReadableWaveletData wavelet) {
          return true;
        }
      };

  public static String buildUserQuery(String query) {
    return query.replaceAll(WORD_START + TokenQueryType.IN.getToken() + ":", IN + ":")
        .replaceAll(WORD_START + TokenQueryType.WITH.getToken() + ":", WITH_FUZZY + ":")
        .replaceAll(WORD_START + TokenQueryType.CREATOR.getToken() + ":", CREATOR + ":");
  }

  @Inject
  public SolrSearchProviderImpl(WaveDigester digester, WaveMap waveMap,
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) String waveDomain) {
    super(waveDomain, digester, waveMap, null);
  }

  @Override
  public SearchResult search(final ParticipantId user, String query, int startAt, int numResults) {
    LOG.fine("Search query '" + query + "' from user: " + user + " [" + startAt + ", "
        + (startAt + numResults - 1) + "]");

    /*-
     * see
     * org.waveprotocol.box.server.waveserver.SimpleSearchProviderImpl.search(ParticipantId, String, int, int).isAllQuery
     */
    // Maybe should be changed in case other folders in addition to 'inbox' are
    // added.
    final boolean isAllQuery = isAllQuery(query);

    Multimap<WaveId, WaveletId> currentUserWavesView = HashMultimap.create();

    if (numResults > 0) {

      int start = startAt;
      int rows = Math.max(numResults, ROWS);

      /*-
       * "fq" stands for Filter Query. see
       * http://wiki.apache.org/solr/CommonQueryParameters#fq
       */
      String fq = buildFilterQuery(query, isAllQuery, user.getAddress(), sharedDomainParticipantId);

      GetMethod getMethod = new GetMethod();
      try {
        while (true) {
          getMethod.setURI(new URI(SOLR_BASE_URL + "/select?wt=json" + "&start=" + start + "&rows="
              + rows + "&q=" + Q + "&fq=" + fq, false));

          HttpClient httpClient = new HttpClient();
          int statusCode = httpClient.executeMethod(getMethod);
          if (statusCode != HttpStatus.SC_OK) {
            LOG.warning("Failed to execute query: " + query);
            return digester.generateSearchResult(user, query, null);
          }

          JsonObject json =
              new JsonParser().parse(new InputStreamReader(getMethod.getResponseBodyAsStream()))
                  .getAsJsonObject();
          JsonObject responseJson = json.getAsJsonObject("response");
          JsonArray docsJson = responseJson.getAsJsonArray("docs");
          if (docsJson.size() == 0) {
            break;
          }

          Iterator<JsonElement> docJsonIterator = docsJson.iterator();
          while (docJsonIterator.hasNext()) {
            JsonObject docJson = docJsonIterator.next().getAsJsonObject();

            /*
             * TODO (Frank R.) c.f.
             * org.waveprotocol.box.server.waveserver.SimpleSearchProviderImpl
             * .isWaveletMatchesCriteria(ReadableWaveletData, ParticipantId,
             * ParticipantId, List<ParticipantId>, List<ParticipantId>, boolean)
             */

            WaveId waveId = WaveId.deserialise(docJson.getAsJsonPrimitive(WAVE_ID).getAsString());
            WaveletId waveletId =
                WaveletId.deserialise(docJson.getAsJsonPrimitive(WAVELET_ID).getAsString());
            currentUserWavesView.put(waveId, waveletId);

            /*-
             * XXX (Frank R.) (experimental and disabled) reduce round trips to solr
             *
             * the result list will be filtered. so we need all results
             */
            // if (currentUserWavesView.size() >= numResults) {
            // break;
            // }
          }

          /*-
           * XXX (Frank R.) (experimental and disabled) reduce round trips to solr
           *
           * the result list will be filtered. so we need all results
           */
          // if (currentUserWavesView.size() >= numResults) {
          // break;
          // }

          /*
           * there won't be any more results - stop querying next page of
           * results
           */
          if (docsJson.size() < rows) {
            break;
          }

          start += rows;
        }

      } catch (IOException e) {
        LOG.warning("Failed to execute query: " + query);
        return digester.generateSearchResult(user, query, null);
      } finally {
        getMethod.releaseConnection();
      }
    }

    Map<WaveId, WaveViewData> results =
        filterWavesViewBySearchCriteria(matchesFunction, currentUserWavesView);
    if (LOG.isFineLoggable()) {
      for (Map.Entry<WaveId, WaveViewData> e : results.entrySet()) {
        LOG.fine("filtered results contains: " + e.getKey());
      }
    }

    Collection<WaveViewData> searchResult = computeSearchResult(user, startAt, numResults, results);
    LOG.info("Search response to '" + query + "': " + searchResult.size() + " results, user: "
        + user);
    return digester.generateSearchResult(user, query, searchResult);
  }

  public static boolean isAllQuery(String query) {
    return !IN_PATTERN.matcher(query).find();
  }

  public static String buildFilterQuery(String query, final boolean isAllQuery,
      String addressOfRequiredParticipant, ParticipantId sharedDomainParticipantId) {

    String fq;
    if (isAllQuery) {
      fq =
          FILTER_QUERY_PREFIX + "(" + addressOfRequiredParticipant + " OR "
              + sharedDomainParticipantId + ")";
    } else {
      fq = FILTER_QUERY_PREFIX + addressOfRequiredParticipant;
    }
    if (query.length() > 0) {
      fq += " AND (" + buildUserQuery(query) + ")";
    }

    return fq;
  }

  /*-
   * copied with modification from
   * org.waveprotocol.box.server.waveserver.SimpleSearchProviderImpl.computeSearchResult(ParticipantId, int, int, Map<TokenQueryType, Set<String>>, Map<WaveId, WaveViewData>)
   *
   * removed queryParams
   */
  private Collection<WaveViewData> computeSearchResult(final ParticipantId user, int startAt,
      int numResults, Map<WaveId, WaveViewData> results) {
    List<WaveViewData> searchResultslist = null;
    int searchResultSize = results.values().size();
    // Check if we have enough results to return.
    if (searchResultSize < startAt) {
      searchResultslist = Collections.emptyList();
    } else {
      int endAt = Math.min(startAt + numResults, searchResultSize);
      searchResultslist = new ArrayList<WaveViewData>(results.values()).subList(startAt, endAt);
    }
    return searchResultslist;
  }
}
