package org.swellrt.server.box.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationRegisterStoreFake implements NotificationRegisterStore {

  HashMap<String, List<String>> notificationStore;

  public NotificationRegisterStoreFake() {
    notificationStore = new HashMap<String, List<String>>();
  }

  @Override
  public List<String> getSubscriptors(String waveId) {
    return notificationStore.get(waveId);
  }

  @Override
  public void addSubscriptor(String waveId, String userId) {

    List<String> subscriptors = notificationStore.get(waveId);

    if (subscriptors == null) {
      subscriptors = new ArrayList<String>();
      notificationStore.put(waveId, subscriptors);
    }

    if (subscriptors.indexOf(userId) == -1) {
      subscriptors.add(userId);
    }
    System.out.println(subscriptors.toString());
  }

  @Override
  public void removeSubscriptor(String waveId, String userId) {

    List<String> subscriptors = notificationStore.get(waveId);

    if (subscriptors != null) {
      subscriptors.remove(userId);
      System.out.println(subscriptors.toString());
    }
  }

}
