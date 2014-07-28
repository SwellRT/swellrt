package org.waveprotocol.wave.model.extended.type.chat;

import org.waveprotocol.wave.model.util.Serializer;


public class ChatPresenceStatus {

  public static class StatusSerializer implements Serializer<ChatPresenceStatus> {

    private static final String SEPARATOR = ":";

    @Override
    public String toString(ChatPresenceStatus x) {
      return x.status + SEPARATOR + String.valueOf(x.timestamp);
    }

    @Override
    public ChatPresenceStatus fromString(String s) {
      if (s == null || !s.contains(SEPARATOR)) return null;

      try {
        String[] tokens = s.split(SEPARATOR);
        Long timestamp = Long.parseLong(tokens[1]);
        String status = tokens[0];
        return new ChatPresenceStatus(status, timestamp);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    @Override
    public ChatPresenceStatus fromString(String s, ChatPresenceStatus defaultValue) {
      ChatPresenceStatus cps = fromString(s);
      if (cps == null) return defaultValue;
      return cps;
    }

  }

  public static final String STATUS_PREFIX_WRITING = "w";
  public static final String STATUS_PREFIX_ONLINE = "on";

  private static final long MAX_TIME_INTERVAL = 4000;

  private String status;
  private Long timestamp;

  public static ChatPresenceStatus createWritingStatus() {
    return new ChatPresenceStatus(STATUS_PREFIX_WRITING);
  }

  public static ChatPresenceStatus createOnlineStatus() {
    return new ChatPresenceStatus(STATUS_PREFIX_ONLINE);
  }

  protected ChatPresenceStatus(String status) {
    this.status = status;
    this.timestamp = System.currentTimeMillis();
  }

  protected ChatPresenceStatus(String status, Long timestamp) {
    this.status = status;
    this.timestamp = timestamp;
  }

  public boolean isWriting() {
    long now = System.currentTimeMillis();
    return status.equals(STATUS_PREFIX_WRITING) && (now - timestamp) <= MAX_TIME_INTERVAL;
  }

  public boolean isOnline() {
    long now = System.currentTimeMillis();
    return status.equals(STATUS_PREFIX_ONLINE) && (now - timestamp) <= MAX_TIME_INTERVAL;
  }

  protected Long getTimestamp() {
    return this.timestamp;
  }

  protected String getStatus() {
    return this.status;
  }

}
