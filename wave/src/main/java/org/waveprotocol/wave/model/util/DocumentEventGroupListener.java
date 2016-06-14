package org.waveprotocol.wave.model.util;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;

/**
 * Interface for listening when a group of events are going to be deliver by the
 * {@link DocumentEventRouter}.
 * 
 * Groups are explicity generated when using
 * {@link MutableDocument#beginMutationGroup()} and
 * {@link MutableDocument#endMutationGroup()}.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public interface DocumentEventGroupListener {

  /**
   * A group of events is going to be triggered.
   * 
   * @param groupId
   */
  public void onBeginEventGroup(String groupId);

  /**
   * No more events are going to be triggered for the group.
   * 
   * @param groupId
   */
  public void onEndEventGroup(String groupId);
}
