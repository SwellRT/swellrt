package org.swellrt.model.doodad;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

import org.swellrt.api.js.generic.AdapterTypeJS;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.Type;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;

import java.util.Map;

/**
 * Generic Wave Doodad named Widget backed up by a data model object. It
 * delegates rendering and behavior to native JavaScript methods passed as
 * {@link WidgetController}
 * 
 * The Generic Doodad has two attributes:
 * 
 * <li>Type: a string constant to differentiate each widget.</li> <li>Path: the
 * path to the data model object storing the actual state</li>
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public class WidgetModelDoodad {

  public static final String TAG = "widget-model";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_PATH = "path";


  static class RendererHandler extends RenderingMutationHandler {


    final Map<String, WidgetController> controllers;
    final Model model;

    public RendererHandler(Map<String, WidgetController> controllers, Model model) {
      this.controllers = controllers;
      this.model = model;
    }

    @Override
    public Element createDomImpl(Renderable element) {
      Element widgetSpan = Document.get().createSpanElement();
      widgetSpan.addClassName("sw-widget");
      DomHelper.setContentEditable(widgetSpan, false, false);
      return widgetSpan;
    }

    @Override
    public void onActivatedSubtree(final ContentElement element) {

      // Register a listener in the data model object to wire it to the widget

      String path = element.getAttribute(ATTR_PATH);
      final Type dataModelObject = model.fromPath(path);
      final JavaScriptObject dataModelJSObject = AdapterTypeJS.adapt(dataModelObject);

      String type = element.getAttribute(ATTR_TYPE);
      final WidgetController controller = controllers.get(type);
      if (controller == null) return;

      if (dataModelObject instanceof StringType) {
        ((StringType) dataModelObject).addListener(new StringType.Listener() {

          @Override
          public void onValueChanged(String oldValue, String newValue) {
            controller.onChangeState(element.getImplNodelet(), dataModelJSObject);

          }
        });
      } else if (dataModelObject instanceof ListType) {
        ((ListType) dataModelObject).addListener(new ListType.Listener() {

          @Override
          public void onValueRemoved(Type entry) {
            controller.onChangeState(element.getImplNodelet(), dataModelJSObject);
          }

          @Override
          public void onValueAdded(Type entry) {
            controller.onChangeState(element.getImplNodelet(), dataModelJSObject);
          }
        });
      } else if (dataModelObject instanceof MapType) {
        ((MapType) dataModelObject).addListener(new MapType.Listener() {

          @Override
          public void onValueChanged(String key, Type oldValue, Type newValue) {
            controller.onChangeState(element.getImplNodelet(), dataModelJSObject);
          }

          @Override
          public void onValueRemoved(String key, Type value) {
            controller.onChangeState(element.getImplNodelet(), dataModelJSObject);
          }
        });
      }

      // Populate data model object to widget UI

      controller.onInit(element.getImplNodelet(), dataModelJSObject);


    }

    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
        String newValue) {
      // Nothing to do. We don't expect a change in the path attribute
    }
  }


  static class EventHandler extends NodeEventHandlerImpl {


    final Map<String, WidgetController> controllers;
    final Model model;

    public EventHandler(Map<String, WidgetController> controllers, Model model) {
      this.controllers = controllers;
      this.model = model;
    }

    @Override
    public void onActivated(ContentElement element) {

      final String type = element.getAttribute(ATTR_TYPE);
      final WidgetController controller = controllers.get(type);

      String path = element.getAttribute(ATTR_PATH);
      final Type dataModelObject = model.fromPath(path);
      final JavaScriptObject dataModelJSObject = AdapterTypeJS.adapt(dataModelObject);

      // Register Widget's event handlers as Doodad handlers.

      for (int i = 0; i < controller.getSupportedEvents().length(); i++) {
        final String event = controller.getSupportedEvents().get(i);

        // TODO check if event is a valid event

        Helper.registerJsHandler(element, element.getImplNodelet(), event,
            new DomHelper.JavaScriptEventListener() {

              @Override
              public void onJavaScriptEvent(String name, Event event) {
                controller.onEvent(name, event, dataModelJSObject);
              }

            });
      }

    }

    @Override
    public void onDeactivated(ContentElement element) {
      // Cleanup
      Helper.removeJsHandlers(element);
    }

  }


  public static void register(ElementHandlerRegistry registry,
      Map<String, WidgetController> controllers, Model model) {

    RendererHandler renderer = new RendererHandler(controllers, model);
    EventHandler eventHandler = new EventHandler(controllers, model);

    registry.registerRenderingMutationHandler(TAG, renderer);
    registry.registerEventHandler(TAG, eventHandler);
  }

}
