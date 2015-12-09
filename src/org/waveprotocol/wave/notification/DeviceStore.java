package org.waveprotocol.wave.notification;

import java.util.List;

public interface DeviceStore {

  void register(String userId, String deviceId);

  void unregister(String userId, String deviceId);

  List<String> getUserDevices(String userId);

}
