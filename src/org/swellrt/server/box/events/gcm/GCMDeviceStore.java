package org.swellrt.server.box.events.gcm;

import java.util.List;

public interface GCMDeviceStore {

  void register(String userId, String deviceId);

  void unregister(String userId, String deviceId);

  List<String> getUserDevices(String userId);

}
