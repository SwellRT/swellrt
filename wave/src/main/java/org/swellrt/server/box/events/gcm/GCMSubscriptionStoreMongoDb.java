package org.swellrt.server.box.events.gcm;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.waveprotocol.box.server.persistence.mongodb.MongoDbProvider;

import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.typesafe.config.Config;

public class GCMSubscriptionStoreMongoDb implements GCMSubscriptionStore {

  private static final String SUBSCRIPTIONS_KEY = "subscriptions";
  private static final String TARGETS_KEY = "targets";
  private static final String GCM_KEY = "gcm";
  private static final String DEVICES_ID = "devices";
  private static final String UNSUBSCRIBED_SOURCES_KEY = "unsubscribed_sources";
  private static final String WAVE_ID = "waveId";
  private static final String WAVE_ID_KEY = "wave_id";
  private static final BasicDBObject gcmDevicesProjection = new BasicDBObject(
      SUBSCRIPTIONS_KEY + "." + TARGETS_KEY + "." + GCM_KEY + "." + DEVICES_ID, 1);
  private static final String PARTICIPANTS_KEY = "participants";
  private DBCollection accountStore;
  private DBCollection modelStore;

  private static final Logger LOG = Logger.getLogger(GCMSubscriptionStoreMongoDb.class.getName());


  @Inject
  public GCMSubscriptionStoreMongoDb(MongoDbProvider mongoDbProvider, Config config) {
	String accountStoreType = config.getString("core.account_store_type");
	
    if (accountStoreType.equalsIgnoreCase("mongodb")) {
      this.accountStore = mongoDbProvider.getDBCollection("account");
      this.modelStore = mongoDbProvider.getDBCollection("models");
    } else {
      LOG.warning("Account store type is: \"" + accountStoreType
          + "\" instead of \"mongodb\". GCM Notifications will not work");
    }

  }

  @Override
  public void addSubscriptor(String waveId, String userId) {

    BasicDBList sources = getSources(userId);

    BasicDBObject s = new BasicDBObject(WAVE_ID, waveId);

    sources.remove(s);

    assert (!sources.contains(s));

    setSources(userId, sources);

  }

  @Override
  public void removeSubscriptor(String waveId, String userId) {
    BasicDBList sources = getSources(userId);

    BasicDBObject s = new BasicDBObject(WAVE_ID, waveId);

    if (sources == null) {
      sources = new BasicDBList();
    }
    if (!sources.contains(s)) {
      sources.add(s);
      setSources(userId, sources);
    }
  }

  private void setSources(String userId, BasicDBList sources) {

    BasicDBObject o =
        new BasicDBObject(SUBSCRIPTIONS_KEY + "." + UNSUBSCRIBED_SOURCES_KEY, sources);

    BasicDBObject q = new BasicDBObject();
    q.append("_id", userId);

    accountStore.update(q, new BasicDBObject("$set", o));

  }

  private BasicDBList getSources(String userId) {

    BasicDBObject query = new BasicDBObject();
    query.append("_id", userId);

    DBObject found =
 accountStore.findOne(query,
        new BasicDBObject(SUBSCRIPTIONS_KEY + "." + UNSUBSCRIBED_SOURCES_KEY, 1));

    BasicDBList s;

    try {
      DBObject subs = (DBObject) found.get(SUBSCRIPTIONS_KEY);
      s = (BasicDBList) subs.get(UNSUBSCRIBED_SOURCES_KEY);
    } catch (NullPointerException e) {
      s = new BasicDBList();
    } catch (ClassCastException e) {
      s = new BasicDBList();
    }

    if (s == null) {
      s = new BasicDBList();
    }
    return s;
  }

  @Override
  public List<String> getSubscriptorsDevices(String waveId) {

    BasicDBObject participantsProjection = new BasicDBObject(PARTICIPANTS_KEY, 1);
    BasicDBObject waveIdQuery = new BasicDBObject(WAVE_ID_KEY, waveId);

    DBObject waveData = modelStore.findOne(waveIdQuery, participantsProjection);

    BasicDBList participantAccounts = (BasicDBList) waveData.get(PARTICIPANTS_KEY);

    BasicDBObject inPraticipantsQuery = new BasicDBObject("$in", participantAccounts);

    BasicDBObject subscribedQuery = new BasicDBObject(
SUBSCRIPTIONS_KEY + "." + UNSUBSCRIBED_SOURCES_KEY + "." + WAVE_ID,
            new BasicDBObject("$ne", waveId));

    subscribedQuery.append("_id", inPraticipantsQuery);

    subscribedQuery.append(SUBSCRIPTIONS_KEY + "." + TARGETS_KEY + "." + GCM_KEY + "." + DEVICES_ID,
        new BasicDBObject("$exists", true));

    DBCursor subscribedAccounts =
        accountStore.find(subscribedQuery, gcmDevicesProjection);

    List<String> result = new ArrayList<String>();

    for (DBObject acc : subscribedAccounts) {
      BasicDBList devices = (BasicDBList) ((BasicDBObject) ((BasicDBObject) ((BasicDBObject) acc
          .get(SUBSCRIPTIONS_KEY)).get(TARGETS_KEY)).get(GCM_KEY)).get(DEVICES_ID);
      for (Object d : devices) {
        result.add((String) d);
      }

    }

    return result;

  }

  @Override
  public void register(String userId, String deviceId) {
    BasicDBList gcmDevices = getGCMDevices(userId);

    if (!gcmDevices.contains(deviceId)) {
      gcmDevices.add(deviceId);
      setGCMDevices(userId, gcmDevices);
    }

  }

  @Override
  public void unregister(String userId, String deviceId) {

    BasicDBList gcmDevices = getGCMDevices(userId);
    gcmDevices.remove(deviceId);
    assert (!gcmDevices.contains(deviceId));

    setGCMDevices(userId, gcmDevices);
  }

  private void setGCMDevices(String userId, BasicDBList gcmDevices) {

    BasicDBObject o = new BasicDBObject(
        SUBSCRIPTIONS_KEY + "." + TARGETS_KEY + "." + GCM_KEY + "." + DEVICES_ID, gcmDevices);

    BasicDBObject q = new BasicDBObject();
    q.append("_id", userId);

    accountStore.update(q, new BasicDBObject("$set", o));

  }

  private BasicDBList getGCMDevices(String userId) {

    BasicDBObject query = new BasicDBObject();
    query.append("_id", userId);

    DBObject o = accountStore.findOne(query, gcmDevicesProjection);

    BasicDBList devs;

    try {
      DBObject subs = (DBObject) o.get(SUBSCRIPTIONS_KEY);
      DBObject targ = (DBObject) subs.get(TARGETS_KEY);
      DBObject gcmTarg = (DBObject) targ.get(GCM_KEY);
      devs = (BasicDBList) gcmTarg.get(DEVICES_ID);
    } catch (NullPointerException e) {
      devs = new BasicDBList();
    }

    return devs;
  }

  @Override
  public List<String> getSubscriptorsDevicesExcludingUser(String waveId, String userId) {

    BasicDBList devs = getGCMDevices(userId);

    List<String> allDevs = getSubscriptorsDevices(waveId);
    allDevs.removeAll(devs);

    return allDevs;
  }

}