package x.swellrt.model;

import x.swellrt.model.live.LiveEvent;

public interface CObject extends CMap {
  
  public interface PresenceListener {
    
    public void onConnect(Participant p);
    
    public void onDisconnect(Participant p);
    
  }
  
  public interface LiveListener {
    
    public void onEvent(LiveEvent event);
    
  }
  
  public void setPresenceListener(PresenceListener listener);
  
  public void setLiveListener(LiveListener listener);
    
  public void addParticipant(Participant participant);
  
  public void removeParticipant();
  
}
