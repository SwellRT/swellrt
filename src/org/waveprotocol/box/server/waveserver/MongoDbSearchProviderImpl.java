package org.waveprotocol.box.server.waveserver;

import com.google.inject.Inject;
import com.google.wave.api.SearchResult;

import org.waveprotocol.box.server.persistence.mongodb.MongoDbIndexStore;
import org.waveprotocol.box.server.waveserver.QueryHelper.InvalidQueryException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Map;
import java.util.Set;

/**
 * Search provider implementation based on {@link MongoDbIndexStore}.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class MongoDbSearchProviderImpl implements SearchProvider {

  private static final Log LOG = Log.get(MongoDbSearchProviderImpl.class);

  private final MongoDbIndexStore indexStore;

  @Inject
  public MongoDbSearchProviderImpl(MongoDbIndexStore indexStore) {
    this.indexStore = indexStore;
  }

  @Override
  public SearchResult search(ParticipantId user, String query, int startAt, int numResults) {

    LOG.fine("Search query '" + query + "' from user: " + user + " [" + startAt + ", "
        + (startAt + numResults - 1) + "]");

    Map<TokenQueryType, Set<String>> queryMap = null;


    try {
      queryMap = QueryHelper.parseQuery(query);
    } catch (InvalidQueryException e) {
      e.printStackTrace();
    }

    return indexStore.queryIndex(user, queryMap, startAt, numResults, new SearchResult(query));
  }

}
