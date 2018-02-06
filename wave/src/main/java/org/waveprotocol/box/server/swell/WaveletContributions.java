package org.waveprotocol.box.server.swell;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.swellrt.beta.common.SwellConstants;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.indexed.SimpleAnnotationSet;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperationVisitor;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.SubmitBlip;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.VersionUpdateOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationVisitor;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.common.base.Preconditions;

/**
 * Implementation of {@link ReadableWaveletContributions} that is feed by
 * {@link TransformedWaveletDelta} instances.
 */
public class WaveletContributions implements ReadableWaveletContributions {

  protected static final String ANNOTATION_KEY = "author";

  public class BlipContributions implements ReadableBlipContributions {

    final String blipId;
    final SimpleAnnotationSet annotations;
    int documentSize = 0;

    protected BlipContributions(String blipId) {
      this.blipId = blipId;
      this.annotations = new SimpleAnnotationSet(null);
    }

    protected void begin() {
      annotations.begin();
    }

    protected void finish() {
      annotations.finish();
    }

    @Override
    public SimpleAnnotationSet getAnnotations() {
      return annotations;
    }

    @Override
    public Iterable<AnnotationInterval<Object>> getIntervals() {
      return annotations.annotationIntervals(0, documentSize,
          CollectionUtils.newStringSet(WaveletContributions.ANNOTATION_KEY));
    }

  }

  protected Map<String, BlipContributions> blipContribsMap = new HashMap<String, BlipContributions>();
  protected HashedVersion version;
  final protected WaveletName waveletName;

  //
  // State of the filter
  //

  private BlipContributions blipContrib;
  private ParticipantId participant;
  boolean annotationStarted = false;

  DocOpCursor docOpFilter = new DocOpCursor() {



    public void ensureStart() {
      if (!annotationStarted) {
        blipContrib.annotations.startAnnotation(ANNOTATION_KEY, participant);
        annotationStarted = true;
      }
    }

    public void ensureEnd() {
      if (annotationStarted) {
        blipContrib.annotations.endAnnotation(ANNOTATION_KEY);
        annotationStarted = false;
      }
    }


    @Override
    public void elementStart(String type, Attributes attrs) {
      ensureStart();
      blipContrib.annotations.insert(1);
      blipContrib.documentSize++;
    }


    @Override
    public void elementEnd() {
      ensureStart();
      blipContrib.annotations.insert(1);
      blipContrib.documentSize++;
    }

    @Override
    public void characters(String chars) {
      ensureStart();
      blipContrib.annotations.insert(chars.length());
      blipContrib.documentSize+=chars.length();

    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
       // nothing to do
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      ensureStart();
      blipContrib.annotations.insert(1);
    }

    @Override
    public void retain(int itemCount) {
      ensureEnd();
      blipContrib.annotations.skip(itemCount);
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      ensureStart();
      blipContrib.annotations.insert(1);
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      ensureStart();
      blipContrib.annotations.delete(1);
      blipContrib.documentSize--;
    }

    @Override
    public void deleteElementEnd() {
      ensureStart();
      blipContrib.annotations.delete(1);
      blipContrib.documentSize--;
    }

    @Override
    public void deleteCharacters(String chars) {
      ensureStart();
      blipContrib.annotations.delete(chars.length());
      blipContrib.documentSize-=chars.length();
    }
  };

  BlipOperationVisitor blipOpVisitor = new BlipOperationVisitor() {

    @Override
    public void visitSubmitBlip(SubmitBlip op) {
      // no-op
    }

    @Override
    public void visitBlipContentOperation(BlipContentOperation op) {
      op.getContentOp().apply(docOpFilter);
    }
  };

  WaveletOperationVisitor waveletOpVisitor = new WaveletOperationVisitor() {

    @Override
    public void visitNoOp(NoOp op) {
      // no-op
    }

    @Override
    public void visitVersionUpdateOp(VersionUpdateOp op) {
      // no-op
    }

    @Override
    public void visitAddParticipant(AddParticipant op) {
      // no-op
    }

    @Override
    public void visitRemoveParticipant(RemoveParticipant op) {
      // no-op
    }

    @Override
    public void visitWaveletBlipOperation(WaveletBlipOperation op) {

      if (!op.getBlipId().startsWith(SwellConstants.TEXT_BLIP_ID_PREFIX)) return;

      participant = op.getContext().getCreator();
      blipContrib = getBlipContrib(op.getBlipId());
      blipContrib.begin();
      op.getBlipOp().acceptVisitor(blipOpVisitor);
      blipContrib.finish();
      annotationStarted = false;
    }

  };



  public WaveletContributions(WaveletName waveletName) {
    this.waveletName = waveletName;
  }


  protected BlipContributions getBlipContrib(String blipId) {
    blipContrib = blipContribsMap.get(blipId);
    if (blipContrib == null) {
      blipContrib = new BlipContributions(blipId);
      blipContribsMap.put(blipId, blipContrib);
    }
    return blipContrib;
  }

  /**
   * For testing
   *
   * @param blipId
   * @param docInit
   */
  protected void init(String blipId, DocInitialization docInit, ParticipantId creatorId) {
    BlipContributions blipContrib = getBlipContrib(blipId);
    blipContrib.annotations.begin();
    blipContrib.annotations.startAnnotation(ANNOTATION_KEY, creatorId);
    docInit.apply(new DocInitializationCursor() {

      @Override
      public void elementStart(String type, Attributes attrs) {
        blipContrib.annotations.insert(1);
      }

      @Override
      public void elementEnd() {
        blipContrib.annotations.insert(1);
      }

      @Override
      public void characters(String chars) {
        blipContrib.annotations.insert(chars.length());
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
      }
    });
    blipContrib.annotations.endAnnotation(ANNOTATION_KEY);
    blipContrib.annotations.finish();
  }


  protected void apply(WaveletBlipOperation blipOp) {
    waveletOpVisitor.visitWaveletBlipOperation(blipOp);
  }


  protected void apply(WaveletOperation op) {
    op.acceptVisitor(waveletOpVisitor);
  }


  public void apply(TransformedWaveletDelta delta) {

    delta.forEach(op -> {
      op.acceptVisitor(waveletOpVisitor);
    });
    version = delta.getResultingVersion();
  }

  public Set<Entry<String, BlipContributions>> getBlipContributions() {
    return blipContribsMap.entrySet();
  }

  @Override
  public BlipContributions getBlipContributions(String blipId) {
    Preconditions.checkNotNull(blipId);
    return this.blipContribsMap.get(blipId);
  }

  @Override
  public HashedVersion getWaveletVersion() {
    return this.version;
  }

}
