package org.swellrt.server.box.events.gcm;

import java.util.List;

public interface GCMSubscriptionManager {

  public List<String> getSubscriptorsDevices(String waveId);

}
