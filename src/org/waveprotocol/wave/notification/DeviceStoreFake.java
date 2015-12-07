package org.waveprotocol.wave.notification;

import org.waveprotocol.box.server.account.AccountData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceStoreFake implements DeviceStore {

  HashMap<String, List<String>> store;

  public DeviceStoreFake() {
    store = new HashMap<String, List<String>>();
  }

  @Override
  public void register(AccountData user, String deviceId) {

    String userId = user.getId().toString();
    List<String> deviceList = store.get(userId);

    if (deviceList == null) {
      deviceList = new ArrayList<String>();
    }

    if (deviceList.indexOf(deviceId) == -1) {
      deviceList.add(deviceId);
      store.put(userId, deviceList);
    }
  }

  @Override
  public void unregister(AccountData user, String deviceId) {

    String userId = user.getId().toString();
    List<String> deviceList = store.get(userId);

    int index = deviceList.indexOf(deviceId);

    if (deviceList != null && index > -1) {
      deviceList.remove(index);
    }

  }

  @Override
  public List<String> getUserDevices(AccountData user) {
    return store.get(user);
  }

}
