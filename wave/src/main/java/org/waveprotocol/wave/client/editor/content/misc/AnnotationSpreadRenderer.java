/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.client.editor.content.misc;

import java.util.HashSet;
import java.util.Set;

import org.waveprotocol.wave.client.common.scrub.Scrub;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ClientDocumentContext;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.EventHandler;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.MutationHandler;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

import jsinterop.annotations.JsFunction;

/**
 * Renderer for the bits of paint that spread over text
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
class AnnotationSpreadRenderer extends RenderingMutationHandler {

  private static final int NOTIFY_SCHEDULE_DELAY_MS = 200;

  private static final int MOUSE_LISTENER_EVENTS = Event.MOUSEEVENTS | Event.ONCLICK;
  
  private final Set<ContentElement> mutatedElements = new HashSet<ContentElement>();

  private final Task mutationNotificationTask = new Task() {
    @Override
    public void execute() {
      for (ContentElement element : mutatedElements) {
        ClientDocumentContext context = element.getContext();
        if (!context.editing().hasEditor()) {
          continue;
        }

        Set<MutationHandler> handlers = getMutationHandlers(element);
        for (MutationHandler h: handlers) {
        	h.onMutation(element);
        }

      }
      mutatedElements.clear();
    }
  };

  private static Set<MutationHandler> getMutationHandlers(ContentElement element) {
	
	Set<MutationHandler> handlers = new HashSet<MutationHandler>();  
	
	
	element.getAttributes().each(new ProcV<String>() {
		@Override
		public void apply(String key, String value) {
			
		  if (key.startsWith(AnnotationPaint.MUTATION_LISTENER_ATTR_PREFIX)) {		    
		    MutationHandler h = AnnotationPaint.mutationHandlerRegistry.get(value);
        if (h != null)
          handlers.add(h);		    
		  }		
		}		
	});
	
	return handlers;
  }

  @Override
  public void onActivationStart(ContentElement element) {
    fanoutAttrs(element);
  }

  @Override
  public void onAttributeModified(final ContentElement element, String name,
      String oldValue, final String newValue) {
    if (name.equals(AnnotationPaint.LINK_ATTR)) {
      // NOTE(user): This is a special case, because it replaces the DOM node,
      // we must reapply all the attributes.
      maybeConvertToAnchor(element, newValue != null);
      element.getAttributes().each(new ProcV<String>() {
        @Override
        public void apply(String key, String value) {
          applyAttribute(element, key, value);
        }
      });
    } else {
      applyAttribute(element, name, newValue);
    }
  }

  private void applyAttribute(ContentElement element, String name, String newValue) {
    // NOTE(user): If an link attribute is added, then handle specially,
    // otherwise treat as style attribute.

    
    Element implNodelet = element.getImplNodelet();
    if (name.equals(AnnotationPaint.LINK_ATTR)) {
      if (newValue != null) {
        String scrubbedValue = Scrub.scrub(newValue);
        implNodelet.setAttribute("href", scrubbedValue);
        if (scrubbedValue.startsWith("#")) {
          implNodelet.removeAttribute("target");
        } else {
          implNodelet.setAttribute("target", "_blank");
        }
      } else {
        implNodelet.removeAttribute("href");
      }
    } else if (name.equals(AnnotationPaint.MOUSE_LISTENER_ATTR)) {
      
      // TODO unify this section with the general case, down below
      updateEventHandler(element, "link", newValue != null && !newValue.isEmpty());
      
    } else if (name.equals(AnnotationPaint.CLASS_ATTR)) {
      // If a class attribute is provided, set as a CSS class name
      implNodelet.addClassName(newValue);
    }
    //
    // Attributes for generic annotations
    //    
    else if (name.startsWith(AnnotationPaint.VALUE_ATTR_PREFIX)) {    	
      String annotationName = name
          .replace(AnnotationPaint.VALUE_ATTR_PREFIX, "");
    	   			
    	implNodelet.addClassName(annotationName);    	
    	implNodelet.setAttribute("data-"+annotationName, newValue);   
    	
    } else if (name.startsWith(AnnotationPaint.EVENT_LISTENER_ATTR_PREFIX)) {   
      
      String annotationName = name
          .replace(AnnotationPaint.EVENT_LISTENER_ATTR_PREFIX, "");
        
      updateEventHandler(element, annotationName, newValue != null && !newValue.isEmpty());  	    
    
    } else if (name.startsWith(AnnotationPaint.MUTATION_LISTENER_ATTR_PREFIX)) {
    	// Ignore mutation listener attributes, not need to paint
    } else { 	
    //
    // Rest of local node attributes are meant to be inline styles
    //	
      try {
        implNodelet.getStyle().setProperty(name, newValue);
      } catch (RuntimeException e) {
        // NOTE(user): some property value are invalid, try catch them and ignores them.
        EditorStaticDeps.logger.error().log("Failed to set CSS property " + name +
          " -> " + newValue);
      }
    }
  }
  
  @JsFunction
  public interface DOMEventListener {
    void exec(Event e);
  }
  
  private native void DOMAddEventListener(Element e, String listenerId, DOMEventListener listener) /*-{
  
    var events = ["click", "mousedown", "mouseup", "mousemove", "mouseover", "mouseout"];
       
    for (var i in events) {
      e.addEventListener(events[i], listener, false);
    }
    
    if (!e.listeners) {
      e.listeners = new Object();
    }
    
    e.listeners[listenerId] = listener;
  
  }-*/;
  
  private native void DOMRemoveEventListener(Element e, String listenerId) /*-{
  
    var events = ["click", "mousedown", "mouseup", "mousemove", "mouseover", "mouseout"];
    
    var listener = e.listeners[listenerId];
    
    if (listener) {   
      for (var i in events) {
        e.removeEventListener(events[i], listener, false);
      }
    
      delete e.listeners[listenerId]; 
    
    }
  
  }-*/;

  /**
   * Allow different handlers in a single node, one for each
   * annotation type (eventHandlerId).
   * <p>
   * The same handler will be registered for all supported events.
   * 
   * @param element
   * @param eventHandlerId
   * @param enable
   */
  private void updateEventHandler(final ContentElement element, String eventHandlerId, boolean enable) {
    Element implNodelet = element.getImplNodelet();
    final EventHandler handler =
        eventHandlerId == null ? null : AnnotationPaint.eventHandlerRegistry.get(eventHandlerId);
    
    if (handler != null && enable) {
      
      DOM.sinkEvents(DomHelper.castToOld(implNodelet), MOUSE_LISTENER_EVENTS);
      
      // Old way
      /*
      DOM.setEventListener(DomHelper.castToOld(implNodelet), new EventListener() {
        @Override
        public void onBrowserEvent(Event event) {
          handler.onEvent(element, event);
        }
      });
      */
      
      /**
       * Allow to have multiple handlers in same node, i.e. same text could have
       * more that one annotations.
       */
      DOMAddEventListener(implNodelet, eventHandlerId, new DOMEventListener() {

        @Override
        public void exec(Event e) {
          handler.onEvent(element, e) ;
        }
        
      });
      
    } else if (!enable) {           
      DOMRemoveEventListener(implNodelet, eventHandlerId);
      // Old way
      // DOM.setEventListener(implNodelet, null);
      DOM.sinkEvents(implNodelet, DOM.getEventsSunk(implNodelet) & ~MOUSE_LISTENER_EVENTS);
    }
  }

  private static Element createHtml(boolean isAnchor) {
    Element e = isAnchor
        ? Document.get().createAnchorElement()
        : Document.get().createSpanElement();

    // Prevents some browsers (to my knowledge, currently just Webkit)
    // from removing empty elements from the dom too much
    if (UserAgent.isWebkit()) {
      e.setAttribute("x", "y");
    }

    return e;
  }

  /**
   * Switches the impl nodelet to and from an anchor element.
   *
   * This is to avoid using anchor elements unless we actually need to render a link. Links
   * generally have strange behaviours in various browsers, and need special (often inefficient)
   * code to deal with them, so the fewer the better.
   *
   * @param toAnchor if true, convert to an anchor, otherwise, convert to a span.
   */
  private void maybeConvertToAnchor(ContentElement element, boolean toAnchor) {
    Element nodelet = element.getImplNodelet();
    boolean isAnchor = nodelet.getTagName().equalsIgnoreCase("a");
    if (isAnchor != toAnchor) {
      removeListener(DomHelper.castToOld(nodelet));
      Element newNodelet = createHtml(toAnchor);

      DomHelper.replaceElement(nodelet, newNodelet);
      element.setBothNodelets(newNodelet);
    }
  }
    
  @Override
  public void onAddedToParent(ContentElement element, ContentElement oldParent) {
    Set<MutationHandler> handlers = getMutationHandlers(element);
    for (MutationHandler h : handlers) {
      h.onAdded(element);
    }
  }  

  @Override
  public void onRemovedFromParent(ContentElement element, ContentElement newParent) {
    if (newParent != null) {
      return;
    }
    
    Set<MutationHandler> handlers = getMutationHandlers(element);
    for (MutationHandler h: handlers) {
      h.onRemoved(element);
    }
    
    removeListener(DomHelper.castToOld(element.getImplNodelet()));
    super.onRemovedFromParent(element, newParent);
  }
  
  @Deprecated
  private void removeListener(com.google.gwt.user.client.Element implNodelet) {
    DOM.setEventListener(implNodelet, null);
    DOM.sinkEvents(implNodelet, DOM.getEventsSunk(implNodelet) & ~MOUSE_LISTENER_EVENTS);
  }

  @Override
  public Element createDomImpl(Renderable element) {
    return element.setAutoAppendContainer(createHtml(false));
  }

  private void scheduleMutationNotification(ContentElement element) {
    Set<MutationHandler> handlers = getMutationHandlers(element);
    if (!handlers.isEmpty()) {
      mutatedElements.add(element);
    }

    Scheduler scheduler = SchedulerInstance.get();
    if (!scheduler.isScheduled(mutationNotificationTask)) {
      scheduler.scheduleDelayed(Priority.MEDIUM, mutationNotificationTask,
          NOTIFY_SCHEDULE_DELAY_MS);
    }
  }

  @Override
  public void onDescendantsMutated(ContentElement element) {
    scheduleMutationNotification(element);
  }
}
