package org.waveprotocol.wave.notification;

import org.waveprotocol.box.server.account.AccountData;

import java.util.List;

public interface DeviceStore {

  void register(AccountData user, String deviceId);

  void unregister(AccountData user, String deviceId);

  List<String> getUserDevices(AccountData user);

}
