package org.waveprotocol.box.server.swell;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.swell.WaveletContributions.BlipContributions;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Provides participant's contribution info for wavelet blips.
 * <p><br><br>
 *
 * GET /contrib/{waveletid}/{blipdid}/{version-hash}/{version-long}
 *
 * <p><br><br>
 * <code>
 * { contributions: [
 *    {
 *      start : 10,
 *      end   : 20,
 *      values : [
 *        { key : "author", value : "alice@example.org" }
 *      ]
 *    },
 *
 *    ...
 *
 *  ]
 *}
 *</code>
 * @author pablojan@apache.org
 *
 */
@Singleton
@SuppressWarnings("serial")
public final class ContributionsServlet extends HttpServlet {

  private static final Log LOG = Log.get(ContributionsServlet.class);

  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;

  HashedVersionFactory HASHER =
      new HashedVersionZeroFactoryImpl(new IdURIEncoderDecoder(new JavaUrlCodec()));

  static class DeltaToContributionsReceiver implements Receiver<TransformedWaveletDelta> {

    final HttpServletResponse response;
    final WaveletContributions contributions;
    final HashedVersion lastVersion;

    public DeltaToContributionsReceiver(WaveletName waveletName, HttpServletResponse response, HashedVersion lastVersion) {
      this.response = response;
      this.contributions = new WaveletContributions(waveletName);
      this.lastVersion = lastVersion;
    }

    @Override
    public boolean put(TransformedWaveletDelta obj) {
      contributions.apply(obj);

      return true;
    }

    public WaveletContributions getContributions() {
      return contributions;
    }

  }


  @Inject
  public ContributionsServlet(
      WaveletProvider waveletProvider, SessionManager sessionManager) {
    this.waveletProvider = waveletProvider;
    this.sessionManager = sessionManager;
  }

  /**
   * Create an http response to the fetch query. Main entrypoint for this class.
   */
  @Override
  @VisibleForTesting
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId user = sessionManager.getLoggedInUser(req);

    if (user == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    // This path will look like "/example.com/w+abc123/foo.com/conv+root
    // Strip off the leading '/'.
    String urlPath = req.getPathInfo().substring(1);

    // Extract the name of the wavelet from the URL
    WaveRef waveref;
    try {
      waveref = JavaWaverefEncoder.decodeWaveRefFromPath(urlPath);

      if (!waveref.hasDocumentId() || !waveref.hasWaveletId()) {
        throw new InvalidWaveRefException(urlPath, "No wavelet or document id");
      }

    } catch (InvalidWaveRefException e) {
      // The URL contains an invalid waveref. There's no document at this path.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    WaveletName waveletName = WaveletName.of(waveref.getWaveId(), waveref.getWaveletId());

    try {
      waveletProvider.checkAccessPermission(waveletName, user);
    } catch (WaveServerException e1) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }


    long versionNumber = -1;
    byte[] versionHash = null;

    // Get the version number from path
    int lastPathElementStart = req.getPathInfo().lastIndexOf("/");
    int lastPathElementEnd = req.getPathInfo().indexOf(";", lastPathElementStart);
    lastPathElementEnd = lastPathElementEnd == -1 ? req.getPathInfo().length() : lastPathElementEnd;
    String versionNumberPathElement = req.getPathInfo().substring(lastPathElementStart + 1, lastPathElementEnd);
    try {
      versionNumber = Long.valueOf(versionNumberPathElement);
    } catch (NumberFormatException e) {
      LOG.info("Can't get wavelet's number version from URL", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    String subPath = req.getPathInfo().substring(0, lastPathElementStart);
    lastPathElementStart = subPath.lastIndexOf("/");
    String versionHashPathElement = subPath.substring(lastPathElementStart + 1);
    try {
      versionHash = CharBase64.decodeWebSafe(versionHashPathElement);
    } catch (Base64DecoderException e) {
      LOG.info("Can't decode wavelet's hashed version from URL", e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    HashedVersion lastVersion = HashedVersion.of(versionNumber, versionHash);

    WaveletContributions contributions = null;
    try {
      LOG.fine("Building wavelet contributions for "+waveletName.toString()+", reading "+lastVersion.getVersion()+" deltas...");
      DeltaToContributionsReceiver deltasToContributions = new DeltaToContributionsReceiver(waveletName, response, lastVersion);
      waveletProvider.getHistory(waveletName, HASHER.createVersionZero(waveletName), lastVersion, deltasToContributions);
      contributions = deltasToContributions.getContributions();
      LOG.fine("Building wavelet contributions, done!");
    } catch (WaveServerException e) {
      LOG.info("Exception retrieven wavelet's deltas", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }


    if (contributions == null) {
      LOG.info("No Wavelet's contributions found");
      response.sendError(HttpServletResponse.SC_NO_CONTENT);
      return;
    }


    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-control", "public, max-age=86400"); // 24h

    final JsonObject jsonResponse = new JsonObject();
    contributions.blipContribsMap.entrySet().forEach( entry -> {
      JsonArray jsonBlipContribs = serializeContribAnnotations(entry.getValue());
      jsonResponse.add(entry.getKey(), jsonBlipContribs);
    });


    Gson gson = new Gson();
    String stringResponse = gson.toJson(jsonResponse);
    response.getWriter().append(stringResponse);

  }

  protected JsonArray serializeContribAnnotations(BlipContributions blipContribs) {


    JsonArray jsonIntervals = new JsonArray();

    blipContribs
      .annotations
      .annotationIntervals(0, blipContribs.documentSize, CollectionUtils.newStringSet(WaveletContributions.ANNOTATION_KEY))
      .forEach(interval -> {

        JsonObject jsonInterval = new JsonObject();
        jsonInterval.addProperty("start", interval.start());
        jsonInterval.addProperty("end", interval.end());
        JsonArray jsonValues = new JsonArray();

        interval.annotations().each(new ProcV<Object>() {
          @Override
          public void apply(String key, Object value) {

            if (value != null) {
              JsonObject pair = new JsonObject();
              pair.addProperty("key", key);
              pair.addProperty("value", value.toString());
              jsonValues.add(pair);
            }
          }
        });

        jsonInterval.add("values", jsonValues);
        jsonIntervals.add(jsonInterval);

      });


    return jsonIntervals;
  }

}
