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

package org.waveprotocol.box.server.persistence.mongodb;

import org.waveprotocol.box.server.persistence.GroupStore;
import org.waveprotocol.box.server.persistence.NamingStore;
import org.waveprotocol.box.server.persistence.PersistenceStartException;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


/**
 * Class to lazily setup and manage the MongoDb connection.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class MongoDbProvider {
  private static final Log LOG = Log.get(MongoDbProvider.class);

  private String dbHost;

  private String dbPort;

  private String dbName;

  /**
   * Our {@link MongoClient} instance, should be accessed by getMongo unless during
   * start().
   */
  private MongoClient mongoClient;

  /**
   * Lazily instantiated {@link MongoDbStore}.
   */
  private MongoDbStore mongoDbStore;

  /**
   * Separated store for Deltas {@link MongoDbDeltaStore}
   */
  private MongoDbDeltaStore mongoDbDeltaStore;

  /**
   * Store for {@link NamingStore}
   */
  private MongoDbNamingStore mongoDbNamingStore;

  /**
   * Store for {@link GroupStore}
   */
  private MongoDbGroupStore mongoDbGroupStore;

  /** Stores whether we have successfully setup a live {@link Mongo} instance. */
  private boolean isRunning;

  /**
   * Constructs a new empty {@link MongoDbProvider}.
   */
  public MongoDbProvider(String dbHost, String dbPort, String dbName) {
    this.dbHost = dbHost;
    this.dbPort = dbPort;
    this.dbName = dbName;
  }

  /**
   * Starts the {@link Mongo} instance and explicitly checks whether it is
   * actually alive.
   *
   * @throws PersistenceStartException if we can't make a connection to MongoDb.
   */
  private void start() {
    Preconditions.checkState(!isRunning(), "Can't start after a connection has been established");

    String host = dbHost;
    int port = Integer.parseInt(dbPort);
    mongoClient = new MongoClient(host, port);

    MongoDatabase database = mongoClient.getDatabase(dbName);

    isRunning = true;
    LOG.info("Started MongoDb persistence");
  }


  /**
   * Returns the {@link DB} with the name that is specified in the properties
   * file.
   */
  private MongoDatabase getDatabase() {
    return getDatabaseForName(dbName);
  }

  /**
   * Returns a {@link MongoDatabase} instance for the database with the given
   * name
   *
   * @param name
   *          the name of the database
   */
  @VisibleForTesting
  MongoDatabase getDatabaseForName(String name) {
    return getMongoClient().getDatabase(name);
  }

  /**
   * Return the {@link Mongo} instance that we are managing.
   */
  private MongoClient getMongoClient() {
    if (!isRunning) {
      start();
    }
    return mongoClient;
  }

  /**
   * Returns true iff the {@link MongoDbProvider} is running.
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Returns a {@link MongoDbStore} instance created from the settings in this
   * provider.
   */
  public MongoDbStore provideMongoDbStore() {
    if (mongoDbStore == null) {
      mongoDbStore = new MongoDbStore(getDatabase());
    }
    return mongoDbStore;
  }

  /**
   * Returns a {@link MongoDbDeltaStore} instance created from the settings in this
   * provider.
   */
  public MongoDbDeltaStore provideMongoDbDeltaStore() {
    if (mongoDbDeltaStore == null) {
      mongoDbDeltaStore = MongoDbDeltaStore.create(getDatabase());
    }

    return mongoDbDeltaStore;

  }

  /**
   * Returns a {@link MongoDbDeltaStore} instance created from the settings in
   * this provider.
   */
  public MongoDbNamingStore provideMongoDbNamingStore() {
    if (mongoDbNamingStore == null) {
      mongoDbNamingStore = MongoDbNamingStore.create(getDatabase());
    }

    return mongoDbNamingStore;

  }

  /**
   * Expose MongoDB collections
   *
   * @param name Collection name
   * @return the DBCollection object
   */
  public MongoCollection<BasicDBObject> getDBCollection(String name) {
    return getDatabase().getCollection(name, BasicDBObject.class);
  }

  public GroupStore provideMongoDbGroupStore() {
    if (mongoDbGroupStore == null) {
      mongoDbGroupStore = MongoDbGroupStore.create(getDatabase());
    }

    return mongoDbGroupStore;
  }
}
