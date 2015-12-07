package org.waveprotocol.wave.notification;

import org.waveprotocol.box.server.account.AccountData;

import java.util.HashMap;
import java.util.List;

public class NotificationRegisterStoreFake implements NotificationRegisterStore {

  HashMap<String, List<AccountData>> notificationStore;

  public NotificationRegisterStoreFake() {
    notificationStore = new HashMap<String, List<AccountData>>();
  }

  @Override
  public List<AccountData> getSubscriptors(String waveId) {
    return notificationStore.get(waveId);
  }

  @Override
  public void addSubscriptor(String waveId, AccountData user) {
    List<AccountData> subscriptors = notificationStore.get(waveId);
    if (subscriptors.indexOf(user) == -1) {
      subscriptors.add(user);
    }
  }

  @Override
  public void removeSubscriptor(String waveId, AccountData user) {
    List<AccountData> subscriptors = notificationStore.get(waveId);
    subscriptors.remove(user);
  }

}
