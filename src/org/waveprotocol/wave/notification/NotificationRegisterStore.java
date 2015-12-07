package org.waveprotocol.wave.notification;

import org.waveprotocol.box.server.account.AccountData;

import java.util.List;

public interface NotificationRegisterStore {

  List<AccountData> getSubscriptors(String waveId);

  void addSubscriptor(String waveId, AccountData user);

  void removeSubscriptor(String waveId, AccountData user);

}
