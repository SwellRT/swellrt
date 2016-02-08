package org.swellrt.server.box.servlet;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;

import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccessModelService implements SwellRTService {

  private static final Log LOG = Log.get(AccessModelService.class);


  public class ResponseData {

    public String waveId;
    public String waveletId;
    public boolean canWrite;
    public boolean canRead;
    public String participantId;

    public ResponseData(String waveId, String waveletId, boolean canWrite, boolean canRead,
        String participantId) {
      super();
      this.waveId = waveId;
      this.waveletId = waveletId;
      this.canWrite = canWrite;
      this.canRead = canRead;
      this.participantId = participantId;
    }


  }

  public static AccessModelService get(ParticipantId participantId, WaveletProvider waveletProvider) {
    return new AccessModelService(participantId, waveletProvider);
  }


  private final ParticipantId participantId;
  private final WaveletProvider waveletProvider;


  public AccessModelService(ParticipantId participantId, WaveletProvider waveletProvider) {
    this.participantId = participantId;
    this.waveletProvider = waveletProvider;
  }


  private void addParticipantToWavelet(final ParticipantId participantId,
      final WaveletName waveletName, WaveletProvider waveletProviver) throws WaveServerException {

    ProtocolWaveletOperation addParticipantOp =
        ProtocolWaveletOperation.newBuilder().setAddParticipant(participantId.getAddress())
            .build();

    ProtocolHashedVersion hashedVersion =
        ProtocolHashedVersion
            .newBuilder()
            .setHistoryHash(
                ByteString.copyFrom(waveletProvider.getSnapshot(waveletName).committedVersion
                    .getHistoryHash()))
            .setVersion(waveletProvider.getSnapshot(waveletName).committedVersion.getVersion())
            .build();

    ProtocolWaveletDelta delta =
        ProtocolWaveletDelta.newBuilder().setAuthor(participantId.getAddress())
            .setHashedVersion(hashedVersion).addOperation(addParticipantOp)
            .build();

    waveletProvider.submitRequest(waveletName, delta, new SubmitRequestListener() {

      @Override
      public void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
          long applicationTimestamp) {
        LOG.info("Granted write access for " + participantId.getAddress() + " to "
            + waveletName.toString());

      }

      @Override
      public void onFailure(String errorMessage) {
        LOG.info("Error adding participant " + participantId.getAddress() + " to "
            + waveletName.toString() + ": " + errorMessage);

      }

    });

  }


  private void sendResponse(HttpServletResponse response, ResponseData respData) throws IOException {

    Gson gson = new Gson();

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");

    response.setHeader("Cache-Control", "no-store");
    response.getWriter().append(gson.toJson(respData));
    response.getWriter().close();

  }

  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    // /access/<mode>/<waveletId>
    // <mode> = write | read

    // get start position of args string, stripping of first / : <mode>/<waveletid>
    String pathInfo = SwellRtServlet.getCleanPathInfo(req);
    int p = pathInfo.indexOf("access") + "access".length() + 1;
    String args = pathInfo.substring(p);


    // extract wavelet id
    String waverRefArg = args.substring(args.indexOf("/")+1);

    // Extract the name of the wavelet from the URL arg
    WaveRef waveref;
    try {
      waveref = JavaWaverefEncoder.decodeWaveRefFromPath(waverRefArg);
    } catch (InvalidWaveRefException e) {
      // The URL contains an invalid waveref. There's no document at this path.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wavelet id invalid syntax");
      return;
    }

    // extract mode
    String accessMode = args.substring(0, args.indexOf("/"));


    if (!accessMode.equalsIgnoreCase("read") && !accessMode.equalsIgnoreCase("write")) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Access mode not valid");
      return;
    }


    final WaveletName waveletName = WaveletName.of(waveref.getWaveId(), waveref.getWaveletId());

    try {

      boolean canWrite = false;
      boolean canRead = false;

      ResponseData respData = null;

      if (waveletProvider.checkAccessPermission(waveletName, participantId)) {

        canRead = true;
        canWrite = true;

        if (!waveletProvider.getSnapshot(waveletName).snapshot.getParticipants().contains(
            participantId)) {

          if (accessMode.equalsIgnoreCase("write")) {
            addParticipantToWavelet(participantId, waveletName, waveletProvider);
          } else {
            canWrite = false;
          }
        }

      }

      respData =
          new ResponseData(ModernIdSerialiser.INSTANCE.serialiseWaveId(waveref.getWaveId()),
              ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveref.getWaveletId()),
              canWrite, canRead, participantId.getAddress());

      sendResponse(response, respData);


    } catch (WaveServerException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }


  }

}
