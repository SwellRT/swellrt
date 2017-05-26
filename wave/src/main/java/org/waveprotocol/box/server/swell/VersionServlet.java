package org.waveprotocol.box.server.swell;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.rpc.ProtoSerializer;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.VersionUpdateOp;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationVisitor;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;

/**
 * GET /version/{waveletid}/{docid}
 *
 * List all versions of the document
 *
 * GET /version/{waveletid}/{docid}/{version:hash}
 *
 * Return snapshot for the version.
 *
 *
 * @author pablojan@apache.org (Pablo Ojanguren)
 *
 */
@SuppressWarnings("serial")
@Singleton
public class VersionServlet extends HttpServlet {
  private static final Log LOG = Log.get(VersionServlet.class);

  private static boolean NO_CACHE_RESPONSE = true;
  private static boolean CACHE_RESPONSE = false;

  private static class BlipRevisionMatcher implements WaveletOperationVisitor {

    final String blipId;

    boolean hasMatched = false;
    ParticipantId author = null;

    public BlipRevisionMatcher(String blipId) {
      this.blipId = blipId;
    }

    public boolean hasMatched() {
      return hasMatched;
    }

    public void reset() {
      hasMatched = false;
      author = null;
    }

    public ParticipantId getAuthor() {
      return author;
    }

    @Override
    public void visitNoOp(NoOp op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitVersionUpdateOp(VersionUpdateOp op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitAddParticipant(AddParticipant op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitRemoveParticipant(RemoveParticipant op) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitWaveletBlipOperation(WaveletBlipOperation op) {
      if (op.getBlipId().equals(blipId)) {
        hasMatched = true;
        author = op.getContext().getCreator();
      }
    }

  }

  private static class HistoryBuilder implements Receiver<TransformedWaveletDelta> {

    final String documentId;
    final BlipRevisionMatcher blipMatcher;
    final JsonWriter writer;

    public HistoryBuilder(String documentId, JsonWriter writer) {
      this.documentId = documentId;
      this.blipMatcher = new BlipRevisionMatcher(documentId);
      this.writer = writer;
    }

    public void begin() throws IOException {
      writer.beginArray();
    }

    public void end() throws IOException {
      writer.endArray();
    }

    @Override
    public boolean put(TransformedWaveletDelta delta) {

      blipMatcher.reset();

      delta.forEach(op -> {
        op.acceptVisitor(blipMatcher);
      });

      if (blipMatcher.hasMatched()) {
        try {
          writer.beginObject();
          writer.name("version").value(delta.getResultingVersion().getVersion());
          writer.name("hash")
              .value(CharBase64.encodeWebSafe(delta.getResultingVersion().getHistoryHash(), true));
          writer.name("author").value(blipMatcher.author.getAddress());
          ZonedDateTime dt = ZonedDateTime.ofInstant(
              Instant.ofEpochMilli(delta.getApplicationTimestamp()), ZoneId.systemDefault());
          writer.name("time").value(dt.toString());
          writer.endObject();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      return true;
    }

  }

  private static class WaveletBuilder implements Receiver<TransformedWaveletDelta> {

    final WaveletName waveletName;
    ObservableWaveletData wavelet;
    boolean started = false;
    boolean wasError = false;


    public WaveletBuilder(WaveletName waveletName) {
      this.waveletName = waveletName;
    }

    public boolean hasError() {
      return wasError;
    }

    public ReadableWaveletData getWavelet() {
      return wavelet;
    }

    @Override
    public boolean put(TransformedWaveletDelta delta) {

      try {

        if (!started) {

          wavelet = WaveletDataUtil.createEmptyWavelet(waveletName, delta.getAuthor(), // creator
              HashedVersion.unsigned(0), // garbage hash, is overwritten by
                                         // first delta below
              delta.getApplicationTimestamp()); // creation time

          WaveletDataUtil.applyWaveletDelta(delta, wavelet);

          started = true;
          return true;

        } else {


          WaveletDataUtil.applyWaveletDelta(delta, wavelet);

        }
      } catch (OperationException e) {
        e.printStackTrace();
        return false;
      }

      return true;
    }

  }

  HashedVersionFactory HASHER = new HashedVersionZeroFactoryImpl(
      new IdURIEncoderDecoder(new JavaUrlCodec()));

  private final ProtoSerializer serializer;
  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;

  @Inject
  public VersionServlet(WaveletProvider waveletProvider, ProtoSerializer serializer,
      SessionManager sessionManager) {
    this.waveletProvider = waveletProvider;
    this.serializer = serializer;
    this.sessionManager = sessionManager;
  }

  /**
   *
   */
  @Override
  @VisibleForTesting
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {

    /*
     * ParticipantId user = sessionManager.getLoggedInUser(req);
     *
     * if (user == null) { response.sendError(HttpServletResponse.SC_FORBIDDEN);
     * return; }
     */

    // This path will look like "/example.com/w+abc123/foo.com/conv+root
    // Strip off the leading '/'.
    String urlPath = req.getPathInfo().substring(1);
    String[] urlPathParts = urlPath.split("/");

    // Extract the name of the wavelet from the URL
    WaveRef waveref;
    try {
      waveref = JavaWaverefEncoder.decodeWaveRefFromPath(urlPath);
    } catch (InvalidWaveRefException e) {
      // The URL contains an invalid waveref. There's no document at this path.
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (!waveref.hasDocumentId()) {
      doGetInfo(waveref, response);
      return;
    }

    long versionNumber = -1;
    byte[] versionHash = null;
    boolean hasVersion = false;


    // Get the version number from path
    if (urlPathParts.length > 5) {

      String[] versionParts = urlPathParts[5].split(":");
      String versionPart = versionParts[0];
      String hashPart = versionParts[1];

      int semicolonIndex = versionPart.indexOf(";");
      if (semicolonIndex != -1)
        versionPart = versionPart.substring(0, semicolonIndex);

      try {
        versionNumber = Long.valueOf(versionPart);
        versionHash = CharBase64.decodeWebSafe(hashPart);
        hasVersion = true;
      } catch (NumberFormatException e) {
        LOG.info("Can't get wavelet's number version from URL", e);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      } catch (Base64DecoderException e) {
        LOG.info("Can't get wavelet's hash version from URL", e);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }

    }

    if (!hasVersion)
      doGetVersionHistory(waveref, response);
    else {
      doGetContent(waveref, HashedVersion.of(versionNumber, versionHash), response);
    }

  }

  protected void doGetVersionHistory(WaveRef waveRef, HttpServletResponse response)
      throws IOException {


    JsonWriter writer = getResponseJsonWriter(response, NO_CACHE_RESPONSE);
    writer.beginObject();

    writer.name("waveId").value(ModernIdSerialiser.INSTANCE.serialiseWaveId(waveRef.getWaveId()));
    writer.name("waveletId")
        .value(ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveRef.getWaveletId()));
    writer.name("blipId").value(waveRef.getDocumentId());
    writer.name("history");

    HistoryBuilder historyBuilder = new HistoryBuilder(waveRef.getDocumentId(), writer);

    historyBuilder.begin();

    WaveletName waveletName = WaveletName.of(waveRef.getWaveId(), waveRef.getWaveletId());
    try {
      CommittedWaveletSnapshot committedSnaphot = waveletProvider.getSnapshot(waveletName);
      waveletProvider.getHistory(waveletName, HASHER.createVersionZero(waveletName),
          committedSnaphot.snapshot.getHashedVersion(), historyBuilder);
    } catch (WaveServerException e) {
      LOG.info("Error processing wavelet history", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }

    historyBuilder.end();
    writer.endObject();

    writer.flush();
  }

  protected void doGetInfo(WaveRef waveRef, HttpServletResponse response) throws IOException {

    WaveletName waveletName = WaveletName.of(waveRef.getWaveId(), waveRef.getWaveletId());
    CommittedWaveletSnapshot committedSnaphot = null;

    try {
      committedSnaphot = waveletProvider.getSnapshot(waveletName);
      if (committedSnaphot == null)
        throw new IllegalStateException("Wavelet Not Found");
    } catch (WaveServerException e) {
      LOG.info("Error processing wavelet history", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (IllegalStateException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
      return;
    }


    ReadableWaveletData wavelet = committedSnaphot.snapshot;

    JsonObject data = new JsonObject();
    data.addProperty("version", wavelet.getHashedVersion().getVersion());
    data.addProperty("modTime", wavelet.getLastModifiedTime());
    data.addProperty("createTime", wavelet.getCreationTime());

    JsonArray blips = new JsonArray();
    wavelet.getDocumentIds().forEach(blipId -> {
      JsonObject blipData = new JsonObject();
      blipData.addProperty("blipId", blipId);
      ReadableBlipData blip = wavelet.getDocument(blipId);
      blipData.addProperty("modTime", blip.getLastModifiedTime());
      blipData.addProperty("version", blip.getLastModifiedVersion());
      blips.add(blipData);
    });

    data.add("blips", blips);

    sendResponseJson(response, data, NO_CACHE_RESPONSE);

  }

  protected void doGetContent(WaveRef waveRef, HashedVersion version, HttpServletResponse response)
      throws IOException {

    WaveletName waveletName = WaveletName.of(waveRef.getWaveId(), waveRef.getWaveletId());

    WaveletBuilder waveletBuilder = new WaveletBuilder(waveletName);

    try {
      waveletProvider.getHistory(waveletName, HASHER.createVersionZero(waveletName), version,
          waveletBuilder);
    } catch (WaveServerException e) {
      LOG.info("Error processing wavelet history", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }

    if (waveletBuilder.hasError()) {
      LOG.info("Error building wavelet");
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    ReadableWaveletData wavelet = waveletBuilder.getWavelet();

    ReadableBlipData blip = wavelet.getDocument(waveRef.getDocumentId());

    if (blip == null) {
      LOG.info("Wavelet doesn't contain the blip");
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    sendResponseXml(response, blip.getContent().getMutableDocument().toXmlString(), CACHE_RESPONSE);

  }

  protected void sendResponseJson(HttpServletResponse response, JsonObject data, boolean noCache)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    if (noCache)
      response.setHeader("Cache-Control", "no-store");
    else
      response.setHeader("Cache-control", "public, max-age=86400"); // 24h

    Gson gson = new Gson();
    String stringResponse = gson.toJson(data);
    response.getWriter().append(stringResponse);

  }

  protected void sendResponseXml(HttpServletResponse response, String xml, boolean noCache)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/xml");
    response.setCharacterEncoding("UTF-8");
    if (noCache)
      response.setHeader("Cache-Control", "no-store");
    else
      response.setHeader("Cache-control", "public, max-age=86400"); // 24h

    response.getWriter().append(xml);

  }

  protected JsonWriter getResponseJsonWriter(HttpServletResponse response, boolean noCache)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    if (noCache)
      response.setHeader("Cache-Control", "no-store");
    else
      response.setHeader("Cache-control", "public, max-age=86400"); // 24h

    return new JsonWriter(response.getWriter());
  }

}
