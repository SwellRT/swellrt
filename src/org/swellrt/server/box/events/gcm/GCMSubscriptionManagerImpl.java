package org.swellrt.server.box.events.gcm;

import java.util.ArrayList;
import java.util.List;

public class GCMSubscriptionManagerImpl implements GCMSubscriptionManager {

  private GCMSubscriptionStore notificationStore;
  private GCMDeviceStore deviceStore;

  private List<String> getSubscriptors(String waveId) {
    return notificationStore.getSubscriptors(waveId);
  }

  // public void addSubscriptor(String waveId, AccountData user) {
  // notificationStore.addSubscriptor(waveId, user);
  // }
  //
  // public void removeSubscriptor(String waveId, AccountData user) {
  // notificationStore.removeSubscriptor(waveId, user);
  // }
  //
  // public void registerDevice(AccountData user, String deviceId) {
  // deviceStore.register(user, deviceId);
  // }
  //
  // public void unregisterDevice(AccountData user, String deviceId) {
  // deviceStore.unregister(user, deviceId);
  // }
  //
  private List<String> getUserDevices(String user) {
    return deviceStore.getUserDevices(user);
  }

  @Override
  public List<String> getSubscriptorsDevices(String waveId) {

    List<String> subscriptors = getSubscriptors(waveId);
    List<String> subscDevices = new ArrayList<String>();

    for (String s : subscriptors) {
      subscDevices.addAll(getUserDevices(s));
    }

    return subscDevices;
  }

}
