package org.swellrt.server.box.events.gcm;

import java.util.List;

public interface GCMSubscriptionStore {

  List<String> getSubscriptors(String waveId);

  void addSubscriptor(String waveId, String userId);

  void removeSubscriptor(String waveId, String userId);

}
