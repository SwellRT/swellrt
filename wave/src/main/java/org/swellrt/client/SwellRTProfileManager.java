package org.swellrt.client;

import java.util.Collection;

import org.swellrt.api.ServiceCallback;
import org.swellrt.api.SwellRT;
import org.swellrt.api.SwellRTUtils;
import org.waveprotocol.wave.client.account.impl.AbstractProfileManager;
import org.waveprotocol.wave.client.account.impl.ProfileImpl;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.RequestException;

/**
 * SwellRT implementation for the {@ProfileManager} interface.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SwellRTProfileManager extends AbstractProfileManager {
  
  
  private static native ServiceCallback getProfileCallback(SwellRTProfileManager manager) /*-{
  
  var callback = function(r) {
    
    if (r && r.data) {
      manager.@org.swellrt.client.SwellRTProfileManager::receiveProfile(Lcom/google/gwt/core/client/JsArray;)(r.data);
    } else {
      var message = "Error fetching profiles: ";
      if (r.error)
         message += " "+r.error;
      else
         message += " unknown error";  
         
      console.log("message");
    }
    
    
  };
  
  return callback;

}-*/;

  private final SwellRT service;
  
  public SwellRTProfileManager(SwellRT service) {
    this.service = service;
  }
  
  /**
   * 
   * Bulk request of profiles. This method doesn't update profiles
   * synchronously.
   * 
   * @param participants a set of participant ids
   */
  public void requestProfiles(Collection<ParticipantId> participants) {   
    
    try {
      service.getUserProfile(SwellRTUtils.toJsArray(participants), getProfileCallback(this));
    } catch (RequestException e) {
      GWT.log("Error fetching profiles", e);
    }
  }
  


  /**
   * Adapts JSON response from server and creates or updates profile entries.
   * 
   * @param profilesRaw a native array
   */
  protected void receiveProfile(JsArray<JsoView> profilesRaw) {      
    
    for (int i = 0; i < profilesRaw.length(); i++) {
      
      JsoView profileRaw = profilesRaw.get(i);
      
      ParticipantId id = null;
      try {
        id = ParticipantId.of(profileRaw.getString("id"));
      } catch (InvalidParticipantAddress e)  {
        continue;
      }
      
      String name = profileRaw.getString("name");
      String imageUrl = profileRaw.getString("avatarUrl");
      
      ProfileImpl profile = null;
      
     
    }
 
  
  }

  @Override
  protected void requestProfile(ParticipantId participantId, RequestProfileCallback callback) {
    // TODO Auto-generated method stub
    
  }
  
}
