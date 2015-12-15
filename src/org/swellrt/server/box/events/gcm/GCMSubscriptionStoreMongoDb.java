package org.swellrt.server.box.events.gcm;

import com.google.inject.Inject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import org.waveprotocol.box.server.persistence.mongodb.MongoDbProvider;

import java.util.ArrayList;
import java.util.List;

public class GCMSubscriptionStoreMongoDb implements GCMSubscriptionStore {

  private static final String SUBSCRIPTIONS_KEY = "subscriptions";
  private static final String TARGETS_KEY = "targets";
  private static final String GCM_KEY = "gcm";
  private static final String DEVICES_ID = "devices";
  private static final String SOURCES_KEY = "sources";
  private static final String WAVE_ID = "waveId";
  private static final BasicDBObject subscribedGCMevicesProjection = new BasicDBObject(
      SUBSCRIPTIONS_KEY + "." + TARGETS_KEY + "." + GCM_KEY + "." + DEVICES_ID, 1);
  private DBCollection accountStore;

  @Inject
  public GCMSubscriptionStoreMongoDb(MongoDbProvider mongoDbProvider) {
    this.accountStore = mongoDbProvider.getDBCollection("account");
  }

  @Override
  public void addSubscriptor(String waveId, String userId) {
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

  @Override
  public void removeSubscriptor(String waveId, String userId) {
    BasicDBList sources = getSources(userId);

    BasicDBObject s = new BasicDBObject(WAVE_ID, waveId);

    sources.remove(s);

    assert (!sources.contains(s));

    setSources(userId, sources);
  }

  private void setSources(String userId, BasicDBList sources) {

    BasicDBObject o = new BasicDBObject(SUBSCRIPTIONS_KEY + "." + SOURCES_KEY, sources);

    BasicDBObject q = new BasicDBObject();
    q.append("_id", userId);

    accountStore.update(q, new BasicDBObject("$set", o));

  }

  private BasicDBList getSources(String userId) {

    BasicDBObject query = new BasicDBObject();
    query.append("_id", userId);

    DBObject found =
        accountStore.findOne(query, new BasicDBObject(SUBSCRIPTIONS_KEY + "." + SOURCES_KEY, 1));

    BasicDBList s;

    try {
      DBObject subs = (DBObject) found.get(SUBSCRIPTIONS_KEY);
      s = (BasicDBList) subs.get(SOURCES_KEY);
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

    BasicDBObject query = new BasicDBObject();

    query.append(SUBSCRIPTIONS_KEY,
        new BasicDBObject(SOURCES_KEY, new BasicDBObject(WAVE_ID, waveId)));
    DBCursor subscribedAccounts = accountStore.find(query, subscribedGCMevicesProjection);

    List<String> result = new ArrayList<String>();

    for (DBObject devs : subscribedAccounts) {
      BasicDBList devices = (BasicDBList) devs;
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

    DBObject o = accountStore.findOne(query, subscribedGCMevicesProjection);

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

}