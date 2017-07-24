package org.waveprotocol.wave.client.doodad.annotation;

import java.util.HashMap;
import java.util.Map;

import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;


/**
 * A generic handler to support user defined (non paragraph) annotations.
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 */
public class UserAnnotationHandler implements AnnotationMutationHandler {

  public interface Activator {
    public boolean shouldFireEvent();
  }

  public interface ContentMutationHandler {
    public <N, E extends N, T extends N> void handleMutation(DocumentContext<N, E, T> bundle,
        int start, int end, String key,
        Object newValue);
  }

  private static UserAnnotationHandler handlerInstance = null;

  private final AnnotationPainter painter;


  public static String getSafeKey(String key) {
    return key.replace("/", "-");
  }

  /**
   * Logic to set paint attributes for the annotation (see {@link AnnotationPaint}).
   * Eventually, these attributes are rendered to HTML by {@link AnnotationSpreadRenderer}.
   */
	private static class RenderFunc implements PaintFunction {

		private final String key;
		private final String cssClass;
		private final JsoView cssStyle;

		public RenderFunc(String key, String cssClass, JsoView cssStyle) {
			  this.key = key;
			  this.cssClass = cssClass;
			  this.cssStyle = cssStyle;
		}

    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {

      Map<String, String> ret = new HashMap<String, String>();

      for (String key : from.keySet()) {
        if (key != null && (key.equals(this.key) || key.startsWith(this.key + "/"))) {

          String safeKey = getSafeKey(key);
          String safeValue = from.get(key).toString();

          // <l:s v-comment="1234">
          ret.put(AnnotationPaint.VALUE_ATTR_PREFIX + safeKey, safeValue);

          // <l:s el-comment="comment"> ... </l:s>
          ret.put(AnnotationPaint.EVENT_LISTENER_ATTR_PREFIX + safeKey, key);

          // <l:s ml-comment="comment"> ... </l:s>
          ret.put(AnnotationPaint.MUTATION_LISTENER_ATTR_PREFIX + safeKey, key);

          // <l:s class-comment="?"> .. </l:s>
          if (cssClass != null)
            ret.put(AnnotationPaint.CLASS_ATTR, cssClass);

          if (cssStyle != null) {

            cssStyle.each(new ProcV<String>() {

              @Override
              public void apply(String styleName, String styleValue) {
                ret.put(styleName, styleValue);
              }

            });
          }
        }


      }
      return ret;
    }

  }

	/**
	 *
	 *
	 * @param registries
	 * @param key
	 * @param mutationHanlder
	 * @param eventHandler
	 */
  public static void register(Registries registries, String key, String cssClass, JsoView cssStyle,
      ContentMutationHandler contentMutationHandler, AnnotationPaint.EventHandler domEventHandler,
      AnnotationPaint.MutationHandler domMutationHandler) {

		PainterRegistry painterRegistry = registries.getPaintRegistry();

		if (handlerInstance == null)
		  handlerInstance = new UserAnnotationHandler(painterRegistry.getPainter());

		AnnotationRegistry annotationRegistry = registries.getAnnotationHandlerRegistry();

		annotationRegistry.registerHandler(key, handlerInstance);
    annotationRegistry.registerBehaviour(key, new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));

    // Register painter to update attributes of the local view
    painterRegistry.registerPaintFunction(CollectionUtils.newStringSet(key), new RenderFunc(key, cssClass, cssStyle));

    // TODO (pablojan) Not sure if boundary functions are required
    // painterRegistry.registerBoundaryFunction(,);

    if (contentMutationHandler != null)
      contentMutationHandlers.put(key, contentMutationHandler);

    if (domEventHandler != null)
      AnnotationPaint.registerEventHandler(key, domEventHandler);

    if (domMutationHandler != null)
      AnnotationPaint.setMutationHandler(key, domMutationHandler);


	}

  private static final Map<String, ContentMutationHandler> contentMutationHandlers = new HashMap<String, ContentMutationHandler>();


  public UserAnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }

  /**
   * Get a mutation handler for the key or its prefix
   *
   * @param key
   * @return
   */
  private ContentMutationHandler getMutationHandler(String key) {

    ContentMutationHandler handler = contentMutationHandlers.get(key);

    if (handler == null && key.contains("/")) {
      String prefix = key.substring(0, key.indexOf("/"));
      handler = contentMutationHandlers.get(prefix);
    }

    return handler;

  }


  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
    ContentMutationHandler handler = getMutationHandler(key);
    if (handler != null)
      handler.handleMutation(bundle, start, end, key, newValue);
  }

  public static boolean isAnnotationNode(ContentNode node) {
	  ContentElement element = node.asElement();
	  if (element == null) return false;
	  return element.getTagName().equals(AnnotationPaint.SPREAD_FULL_TAGNAME);
  }

}
