package org.swellrt.client.editor.doodad;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;

import java.util.Map;

/**
 * Generic Wave Doodad named Widget. It delegates rendering and behavior to
 * native JavaScript methods passed as {@link WidgetController}
 *
 * The Generic Doodad has two attributes:
 *
 * <li>Type: a string constant to differentiate each widget.</li> <li>State: the
 * shared state stored in the document</li>
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WidgetDoodad {

  public static final String TAG = "widget";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_STATE = "state";


  static class WidgetContext extends JavaScriptObject {


    public final static native WidgetContext create(ContentElement element) /*-{

      var widgetContext = {

        _element: element,

        getState: function() {
          return @org.swellrt.client.editor.doodad.WidgetDoodad.WidgetContext::getState(Lorg/waveprotocol/wave/client/editor/content/ContentElement;)(element);
        },

        setState: function(state) {
          @org.swellrt.client.editor.doodad.WidgetDoodad.WidgetContext::setState(Lorg/waveprotocol/wave/client/editor/content/ContentElement;Ljava/lang/String;)(element,state);
        }

      };

      return widgetContext;

    }-*/;


    protected WidgetContext() {

    }

    private final static void setState(ContentElement element, String state) {
      element.getMutableDoc().setElementAttribute(element, ATTR_STATE, state);
    }

    private final static String getState(ContentElement element) {
      return element.getMutableDoc().getAttributes(element).get(ATTR_STATE);
    }


  }

  static class RendererHandler extends RenderingMutationHandler {


    final Map<String, WidgetController> controllers;

    public RendererHandler(Map<String, WidgetController> controllers) {
      this.controllers = controllers;
    }

    @Override
    public Element createDomImpl(Renderable element) {
      Element widgetSpan = Document.get().createSpanElement();
      widgetSpan.addClassName("sw-widget");
      DomHelper.setContentEditable(widgetSpan, false, false);
      return widgetSpan;
    }

    @Override
    public void onActivatedSubtree(ContentElement element) {
      String state = element.getAttribute(ATTR_STATE);
      String type = element.getAttribute(ATTR_TYPE);

      element.getImplNodelet().addClassName("sw-widget-" + type);

      WidgetController controller = controllers.get(type);
      if (controller != null) {
        controller.onInit(element.getImplNodelet(), state);
      }

    }

    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
        String newValue) {
      String type = element.getAttribute(ATTR_TYPE);

      if (name.equals(ATTR_STATE)) {
        WidgetController controller = controllers.get(type);
        if (controller != null) {
          controller.onChangeState(element.getImplNodelet(), oldValue, newValue);
        }
      }
    }
  }


  static class EventHandler extends NodeEventHandlerImpl {


    final Map<String, WidgetController> controllers;

    public EventHandler(Map<String, WidgetController> controllers) {
      this.controllers = controllers;
    }

    @Override
    public void onActivated(ContentElement element) {

      /*
       * final String type = element.getAttribute(ATTR_TYPE); final
       * WidgetController controller = controllers.get(type); final
       * WidgetContext context = WidgetContext.create(element);
       */

      // TODO consider to show this method in the WidgetController interface
    }

    @Override
    public void onDeactivated(ContentElement element) {
      // TODO consider to show this method in the WidgetController interface
    }

  }


  public static void register(ElementHandlerRegistry registry,
      Map<String, WidgetController> controllers) {

    RendererHandler renderer = new RendererHandler(controllers);
    EventHandler eventHandler = new EventHandler(controllers);

    registry.registerRenderingMutationHandler(TAG, renderer);
    registry.registerEventHandler(TAG, eventHandler);
  }

}
