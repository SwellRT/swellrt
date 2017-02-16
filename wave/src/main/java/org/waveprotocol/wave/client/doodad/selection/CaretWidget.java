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

package org.waveprotocol.wave.client.doodad.selection;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.DomHelper.JavaScriptEventListener;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Event;

/**
 * DOM implementation of a caret view
 * 
 * (pablojan) The caret view has a marker and
 * a label for the user name.
 * 
 * The marker will be visible always until user
 * stops edition.
 * 
 * The user name's label will be only visible
 * on mouse over.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class CaretWidget implements CaretView {

  /** Resources for the renderer. */
  private static final CaretMarkerResources RESOURCES = GWT.create(CaretMarkerResources.class);
  public static final CaretMarkerResources.Css CSS = RESOURCES.css();
  static {
    StyleInjector.inject(CSS.getText());
  }

  private final Element caretMarkerElement;
  private final Element caretLeft;
  private final Element caretRight;
  private final Element caretLabel;
  private final Element compositionStateSpan;
  private final Text nameTextNode;

  /**
   * Build a DOM element for storing a single user selection element.
   */
  public CaretWidget() {
    
    
    
    compositionStateSpan = Document.get().createSpanElement();
    
    /*
    caretUpper = Document.get().createSpanElement();
    caretUpper.setClassName(CSS.caretUpper());
    caretUpper.getStyle().setPosition(Position.ABSOLUTE);
    caretUpper.appendChild(nameTextNode);
    
    compositionStateSpan = Document.get().createSpanElement();
    compositionStateSpan.setClassName(CSS.compositionState());
    caretUpper.appendChild(compositionStateSpan);
    */
    
    caretMarkerElement = Document.get().createSpanElement();
    caretMarkerElement.setClassName(CSS.caretMarker());
    
    caretLeft = Document.get().createSpanElement();
    caretLeft.setClassName(CSS.caretMarkerLeft());
    caretRight = Document.get().createSpanElement();
    caretRight.setClassName(CSS.caretMarkerRight());

    caretLabel = Document.get().createSpanElement();
    caretLabel.setClassName(CSS.caretLabel());
    caretLabel.getStyle().setVisibility(Visibility.HIDDEN);
    nameTextNode = Document.get().createTextNode("?");
    caretLabel.appendChild(nameTextNode);
    
    caretMarkerElement.appendChild(caretLabel);
    caretMarkerElement.appendChild(caretLeft);
    caretMarkerElement.appendChild(caretRight);
   
    
    DomHelper.makeUnselectable(caretMarkerElement);
    NodeManager.setTransparency(caretMarkerElement, Skip.DEEP);
    
    DomHelper.registerEventHandler(caretMarkerElement, "mouseover", true, new JavaScriptEventListener() {

      @Override
      public void onJavaScriptEvent(String name, Event event) {
        
          caretLabel.getStyle().setVisibility(Visibility.VISIBLE);
          event.stopPropagation();
        
      }
      
    });
    
    DomHelper.registerEventHandler(caretMarkerElement, "mouseout", true, new JavaScriptEventListener() {

      @Override
      public void onJavaScriptEvent(String name, Event event) {
        
          caretLabel.getStyle().setVisibility(Visibility.HIDDEN);
          event.stopPropagation();
        
      }
      
    });
    
  }

  @Override
  public void setName(String name) {
    nameTextNode.setData(name);
  }

  @Override
  public void setCompositionState(String state) {
    compositionStateSpan.setInnerText(state);
  }

  @Override
  public void setColor(RgbColor color) {
    String cssColour = color.getCssColor();
    compositionStateSpan.getStyle().setBorderColor(cssColour);
    caretMarkerElement.getStyle().setBorderColor(cssColour);
    caretRight.getStyle().setBorderColor(cssColour);
    caretLeft.getStyle().setBorderColor(cssColour);
    caretLabel.getStyle().setBackgroundColor(cssColour);
  }

  public Element getElement() {
    return caretMarkerElement;
  }

  @Override
  public void attachToParent(Element parent) {
    
    if (parent == null)
      return;
       
    parent.appendChild(getElement());
    
  }
  
  
}
