package org.swellrt.beta.client;

import org.swellrt.beta.client.js.editor.STextLocalWeb;
import org.swellrt.beta.client.js.editor.STextRemoteWeb;
import org.swellrt.beta.client.js.editor.STextWeb;
import org.swellrt.beta.client.wave.SWaveDocuments;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.swellrt.beta.model.remote.STextRemote;
import org.swellrt.beta.model.remote.SubstrateId;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.wave.Blip;

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
      targetWeb.getContentDocument().consume(source.getOps());
    }
  }
  
  public class WebPlatformFactory implements PlatformBasedFactory {
    
    private final SWaveDocuments<? extends InteractiveDocument> documentRegistry;
    
    public WebPlatformFactory(SWaveDocuments<? extends InteractiveDocument> documentRegistry) {
      this.documentRegistry = documentRegistry;
    }
    
    @Override
    public STextRemote getSTextRemote(SObjectRemote object, SubstrateId substrateId, Blip blip) {
      
      InteractiveDocument idoc = documentRegistry.getTextDocument(substrateId);
      
      if (idoc != null) {
        return new STextRemoteWeb(object, 
            substrateId,blip, idoc.getDocument());
      }
      
      return null;
          
    }
    
  }


  /** Return an instance of STextRemote hiding actual platform-based implementation */
  public STextRemote getSTextRemote(SObjectRemote object, SubstrateId substrateId, Blip blip);
  
  
}
