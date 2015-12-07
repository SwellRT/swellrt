package org.waveprotocol.wave.notification;

import org.waveprotocol.box.server.account.AccountData;

import java.util.ArrayList;
import java.util.List;

public class NotificationRegisterImpl {

  private NotificationRegisterStore notificationStore;
  private DeviceStore deviceStore;

  private List<AccountData> getSubscriptors(String waveId) {
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
  private List<String> getUserDevices(AccountData user) {
    return deviceStore.getUserDevices(user);
  }

  public List<String> getSubscriptorsDevices(String waveId) {

    List<AccountData> subscriptors = getSubscriptors(waveId);
    List<String> subscDevices = new ArrayList<String>();

    for (AccountData s : subscriptors) {
      subscDevices.addAll(getUserDevices(s));
    }

    return subscDevices;
  }

}
