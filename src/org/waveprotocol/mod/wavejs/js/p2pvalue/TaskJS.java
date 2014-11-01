package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.p2pvalue.Task;

/**
 * A JavaScript wrapper for the Task class
 *
 * @author pablojan@gmail.com
 *
 */
public class TaskJS extends JavaScriptObject implements Task.Listener {

      public native static TaskJS create(Task delegate) /*-{

        var jso = {

         _delegate: delegate,

         callbackMap: new Object(),

         eventHandlers: new Object(),

         registerEventHandler: function(event, handler) {
          this.eventHandlers[event] = handler;
         },

         unregisterEventHandler: function(event, handler) {
          this.eventHandlers[event] = null;
         }

       }; // jso

    return jso;

  }-*/;


  protected TaskJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;


  @Override
  public final void onStatusChanged(String status) {
    // TODO Auto-generated method stub

  }

  @Override
  public final void onDeadlineChanged(long deadline) {
    // TODO Auto-generated method stub

  }



}
