package org.swellrt.server.box.events.gcm;

import java.util.List;

public interface GCMSubscriptionStore {

  // TODO waveId as Class WaveId, rename getDevices;
  public List<String> getSubscriptorsDevices(String waveId);

  void addSubscriptor(String waveId, String userId);

  void removeSubscriptor(String waveId, String userId);

  void register(String userId, String deviceId);

  void unregister(String userId, String deviceId);

}
