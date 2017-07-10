package org.swellrt.beta.client;

import org.swellrt.beta.client.js.editor.STextLocalWeb;
import org.swellrt.beta.client.js.editor.STextRemoteWeb;
import org.swellrt.beta.client.js.editor.STextWeb;
import org.swellrt.beta.client.wave.SWaveDocuments;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.common.PathWalker;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.PathNodeExtractor;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SViewBuilder;
import org.swellrt.beta.model.js.JsPathNodeExtractor;
import org.swellrt.beta.model.js.JsViewVisitor;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SWaveNode;
import org.swellrt.beta.model.wave.SWaveNodeManager;
import org.swellrt.beta.model.wave.SWaveText;
import org.swellrt.beta.model.wave.SubstrateId;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.wave.Blip;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A separated place to create platform dependent types of the SwellRT model.
 * <p>
 * The aim of this class is to avoid any platform dependent decision in the rest
 * of the classes of model.* package.
 * <p>
 * Obviously, this class must be adapted for each platform (Web, Android...)
 * <p>
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface PlatformBasedFactory {

  public static STextLocal getSTextLocal(String text) throws SException {
    return STextLocalWeb.create(text);
  }

  public static PlatformBasedFactory getFactory(WaveLoader loader) {
    return new WebPlatformFactory(loader.getDocumentRegistry());
  }

  public static void copySTextContent(SText source, SText target) {
    if (target instanceof STextWeb) {
      STextWeb targetWeb = (STextWeb) target;
      targetWeb.getContentDocument().consume(source.getInitContent());
    }
  }

  static final JsViewVisitor JSVISITOR_NODE = new JsViewVisitor<SNode>();
  static final JsViewVisitor JSVISITOR_NODE_REMOTE = new JsViewVisitor<SWaveNode>();


  public static SViewBuilder getViewBuilderForNode() {
    return new JsViewVisitor<SNode>();
  }

  public static SViewBuilder getViewBuilderForNodeRemote() {
    return new JsViewVisitor<SWaveNode>();
  }

  static final PathNodeExtractor PATH_NODE_EXTRACTOR = new JsPathNodeExtractor();

  public static PathNodeExtractor getPathNodeExtractor() {
    return PATH_NODE_EXTRACTOR;
  }

  public static Object extractNode(JavaScriptObject jso, String path) {

    if (path == null || path.isEmpty())
      return jso;

    PathWalker pathw = new PathWalker(path);

    String pathElement = pathw.nextPathElement();
    while (pathElement != null && !pathElement.isEmpty() && jso != null) {
      jso = JsoView.as(jso).getJso(pathElement);
      pathElement = pathw.nextPathElement();
    }

    return jso;
  }

  public class WebPlatformFactory implements PlatformBasedFactory {

    private final SWaveDocuments<? extends InteractiveDocument> documentRegistry;

    public WebPlatformFactory(SWaveDocuments<? extends InteractiveDocument> documentRegistry) {
      this.documentRegistry = documentRegistry;
    }

    @Override
    public SWaveText getSTextRemote(SWaveNodeManager nodeManager, SubstrateId substrateId,
        Blip blip) {

      InteractiveDocument idoc = documentRegistry.getTextDocument(substrateId);

      if (idoc != null) {
        return new STextRemoteWeb(nodeManager,
            substrateId,blip, idoc);
      }

      return null;

    }



  }


  /** Return an instance of STextRemote hiding actual platform-based implementation */
  public SWaveText getSTextRemote(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip);
}
