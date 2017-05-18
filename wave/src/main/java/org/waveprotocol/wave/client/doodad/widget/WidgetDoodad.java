package org.waveprotocol.wave.client.doodad.widget;

import java.util.Date;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.JsoStringMap;
import org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget;
import org.waveprotocol.wave.client.doodad.widget.jso.JsoWidgetController;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocHelper.NodeAction;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;


/**
 * Doodad for generic Widgets. It delegates rendering and behavior to
 * native JavaScript methods passed as {@link JsoWidgetController}
 *
 * A widget has two attributes:
 *
 * <li>Type: a string constant to for each widget type</li> 
 * <li>State: the shared state stored in the document</li>
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WidgetDoodad {


  public static final String TAG = "widget";
  public static final String CSS_CLASS = "widget";
  public static final String ATTR_STATE = "state";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_ID = "id";
  
  static class WidgetRendererHandler extends RenderingMutationHandler {


    final StringMap<JsoWidgetController> controllers;

    public WidgetRendererHandler(StringMap<JsoWidgetController> controllers) {
      this.controllers = controllers;
    }

    @Override
    public Element createDomImpl(Renderable element) {
      Element widgetElement = Document.get().createDivElement();
      widgetElement.addClassName(CSS_CLASS);
      // DomHelper.setContentEditable(widgetElement, false, false);
      return widgetElement;
    }

    @Override
    public void onActivatedSubtree(ContentElement element) {
      String state = element.getAttribute(ATTR_STATE);
      String type = element.getAttribute(ATTR_TYPE);
      String typeForRender = type.replace("/", "-");
      String id = element.getAttribute(ATTR_ID);

      
	  element.getImplNodelet().addClassName(typeForRender);
	  element.getImplNodelet().setId(id);
	  element.getImplNodelet().setAttribute("data-"+typeForRender, state);

      JsoWidgetController controller = controllers.get(type);
      if (controller != null) {
        controller.onInit(element.getImplNodelet(), state);
      }

    }

    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
        String newValue) {
      String type = element.getAttribute(ATTR_TYPE);

      if (name.equals(ATTR_STATE)) {
        JsoWidgetController controller = controllers.get(type);
        if (controller != null) {
          controller.onChangeState(element.getImplNodelet(), oldValue, newValue);
        }
      }
    }
    

    @Override
    public void onRemovedFromParent(ContentElement element, ContentElement newParent) {
    }    
    
  }
  


  static class WidgetEventHandler extends NodeEventHandlerImpl {


    final StringMap<JsoWidgetController> controllers;

    public WidgetEventHandler(StringMap<JsoWidgetController> controllers) {
      this.controllers = controllers;
    }

    @Override
    public void onActivated(ContentElement element) {
    	String type = element.getAttribute(ATTR_TYPE);
    	JsoWidgetController controller = controllers.get(type);
    	if (controller != null) 
    		controller.onActivated(element.getImplNodelet());
    }

    @Override
    public void onDeactivated(ContentElement element) {
    	String type = element.getAttribute(ATTR_TYPE);
    	JsoWidgetController controller = controllers.get(type);
    	if (controller != null) 
    		controller.onDeactivated(element.getImplNodelet());
    }

    /**
     * Removes the entire widget
     *
     * {@inheritDoc}
     */
    @Override
    public boolean handleDeleteBeforeNode(ContentElement element, EditorEvent event) {
    	EditorStaticDeps.logger.trace().log("Delete before widget", element);
        element.getMutableDoc().deleteNode(element);
        return true;
    }
    
    /**
     * Removes the entire widget
     *
     * {@inheritDoc}
     */
    @Override
    public boolean handleBackspaceAfterNode(ContentElement element, EditorEvent event) {
    	EditorStaticDeps.logger.trace().log("Backspace after widget", element);
        element.getMutableDoc().deleteNode(element);
        return true;
    }

    
    /**
     * Handles a left arrow that occurred with the caret immediately
     * after this node, by moving caret right before the widget
     *
     * {@inheritDoc}
     */
    @Override
    public boolean handleLeftAfterNode(ContentElement element, EditorEvent event) {
      element.getSelectionHelper().setCaret(Point.before(element.getContext().document(), element));          
      return true;
    }

    /**
     * Handles a right arrow that occurred with the caret immediately
     * before this node, by moving caret to right after the widget
     *
     * {@inheritDoc}
     */
    @Override
    public boolean handleRightBeforeNode(ContentElement element, EditorEvent event) {
      element.getSelectionHelper().setCaret(Point.after(element.getContext().document(), element));
      return true;
    }

    @Override
    public boolean handleLeftAtBeginning(ContentElement element, EditorEvent event) {
    	return false;
    }

    @Override
    public boolean handleRightAtEnd(ContentElement element, EditorEvent event) {
    	return false;
    }    
    
    
  }

  private static StringMap<JsoWidgetController> widgetControllers = JsoStringMap.<JsoWidgetController>create();

  public static void register(ElementHandlerRegistry registry,
      StringMap<JsoWidgetController> controllers) {

    widgetControllers = controllers;  
	  
    WidgetRendererHandler renderer = new WidgetRendererHandler(controllers);
    WidgetEventHandler eventHandler = new WidgetEventHandler(controllers);

    registry.registerRenderingMutationHandler(TAG, renderer);
    registry.registerEventHandler(TAG, eventHandler);
  }
  
  /**
   * Insert or append a widget in a document
   * 
   * @param doc the mutable document
   * @param point point where to insert the widget or null to append
   * @param type type of the widget
   * @param state initial state of the widget
   */
	public static ContentElement addWidget(CMutableDocument doc, Point<ContentNode> point, String type, String state) {
		Preconditions.checkArgument(doc != null, "Unable to add widget to a null document");
		Preconditions.checkArgument(type != null && widgetControllers.containsKey(type),
				"Widget type is not registered");

		// For now, the widget id will be a timestamp
		String id = type.replace("/",  "-") + "-" + String.valueOf(new Date().getTime());

		XmlStringBuilder xml = XmlStringBuilder
				.createFromXmlString("<"+TAG+" "+ATTR_TYPE+"='" + type + "' "+ATTR_STATE+"='" + state + "' "+ATTR_ID+"='" + id + "' />");

		ContentElement element = null;
		
		if (point != null)
			element = doc.insertXml(point, xml);
		else
			element = doc.appendXml(xml);
		
		return element != null ? element : null;
	}

	  /**
	   * Do a recursive search bottom-up in the DOM searching the first node matching
	   * the class
	   * 
	   * @param element
	   * @param clazz
	   * @return
	   */
	  public static Element getWidgetElementUp(Element element) {
		  if (element == null)
			  return null;
		  
		  if (element.hasClassName(CSS_CLASS)) {
			  return element;
		  }
		  
		  return getWidgetElementUp(element.getParentElement());	  
	  }
	  
	  /**
	   * Helper method to get a native view of widget based on its DOM element or descendant.
	   * 
	   * @param doc the mutable document
	   * @param domElement the widget element or a descendant
	   */
	  public static JsoWidget getWidget(CMutableDocument doc, Element domElement) {

		  Element widgetDomElement = getWidgetElementUp(domElement);
		  if (widgetDomElement == null)
			  return null;

		  String widgetId = widgetDomElement.getAttribute(ATTR_ID);
		  ContentElement widgetElement = DocHelper.findElementById(doc, doc.getDocumentElement(), widgetId);

		  if (widgetElement == null) 
			  return null;
		  		  
		  return JsoWidget.create(widgetElement.getImplNodelet(), widgetElement);
	  }
	  
	  public static JsArray<JsoWidget> getWidgets(CMutableDocument doc, String type) {
		  
		  @SuppressWarnings("unchecked")
		  JsArray<JsoWidget> widgets = (JsArray<JsoWidget>) JsArray.createArray();
	  
		  DocHelper.traverse(doc, doc.getDocumentElement(), new NodeAction<ContentNode>() {

			@Override
			public void apply(ContentNode node) {
				if (node.isElement()) {
					ContentElement element = node.asElement();
					if (element.getTagName().equals(TAG) &&
						element.getAttribute(ATTR_TYPE).equals(type)){
						widgets.push(JsoWidget.create(element.getImplNodelet(), element));
					}
				}
			}
			  
		  });
		  
		return widgets;  
	  }

	
}
