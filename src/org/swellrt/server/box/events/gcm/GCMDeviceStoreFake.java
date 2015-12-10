package org.swellrt.server.box.events.gcm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GCMDeviceStoreFake implements GCMDeviceStore {

  HashMap<String, List<String>> store;

  public GCMDeviceStoreFake() {
    store = new HashMap<String, List<String>>();
  }

  @Override
  public void register(String userId, String deviceId) {

    List<String> deviceList = store.get(userId);

    if (deviceList == null) {
      deviceList = new ArrayList<String>();
      store.put(userId, deviceList);
    }

    if (deviceList.indexOf(deviceId) == -1) {
      deviceList.add(deviceId);
      store.put(userId, deviceList);
    }
  }

  @Override
  public void unregister(String userId, String deviceId) {

    List<String> deviceList = store.get(userId);

    if (deviceList != null) {
      int index = deviceList.indexOf(deviceId);
      deviceList.remove(index);
    }
  }

  @Override
  public List<String> getUserDevices(String userId) {
    return store.get(userId);
  }

}
