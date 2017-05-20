package org.waveprotocol.box.server.swell;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.swell.ContributionsServlet;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.impl.PluggableMutableDocument;

import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

public class ContributionsServletTest extends TestCase {

  static class FakeWaveletProvider implements WaveletProvider {

    List<TransformedWaveletDelta> deltas;

    public FakeWaveletProvider(List<TransformedWaveletDelta> deltas) {
      this.deltas = deltas;
    }

    @Override
    public void initialize() throws WaveServerException {
    }

    @Override
    public void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
        SubmitRequestListener listener) {
    }

    @Override
    public void getHistory(WaveletName waveletName, HashedVersion versionStart,
        HashedVersion versionEnd, Receiver<TransformedWaveletDelta> receiver)
        throws WaveServerException {

      deltas.forEach( delta -> {
        receiver.put(delta);
      });


    }

    @Override
    public boolean checkAccessPermission(WaveletName waveletName, ParticipantId participantId)
        throws WaveServerException {
      return true;
    }

    @Override
    public ExceptionalIterator<WaveId, WaveServerException> getWaveIds()
        throws WaveServerException {
      return null;
    }

    @Override
    public ImmutableSet<WaveletId> getWaveletIds(WaveId waveId) throws WaveServerException {
      return null;
    }

    @Override
    public CommittedWaveletSnapshot getSnapshot(WaveletName waveletName)
        throws WaveServerException {
      // TODO Auto-generated method stub
      return null;
    }

  }


  final static byte[] JUNK_HASH = new byte[] {0, 1, 2, 3, 4, 5, -128, 127};

  private WaveletProvider waveletProvider;
  private SessionManager sessionManager;
  private ContributionsServlet servlet;
  private HttpSession session;

  List<TransformedWaveletDelta> generatedDeltas;
  WaveletOperationContext.Factory opContextFactory;
  ParticipantId currentParticipantId;
  Document document;

  protected static Document createStubDocument(List<TransformedWaveletDelta> generatedDeltas, WaveletOperationContext.Factory opContextFactory, String initContent)
  {
    PluggableMutableDocument document;

    DocInitialization docInit = BasicFactories.createDocument(DocumentSchema.NO_SCHEMA_CONSTRAINTS).toInitialization();
    document = BasicFactories.pluggableMutableDocumentFactory().create(WaveletId.of("local.net", "dummy"), "b+dummy", docInit);
    document.init(new SilentOperationSink<DocOp> () {

     @Override
     public void consume(DocOp op) {

       WaveletOperationContext opContext = opContextFactory.createContext();
       BlipOperation blipOp = new BlipContentOperation(opContext, op);
       WaveletBlipOperation waveletOp = new WaveletBlipOperation("b+dummy", blipOp);


       generatedDeltas.add(
           new TransformedWaveletDelta(opContext.getCreator(), opContext.getHashedVersion(), opContext.getTimestamp(), Collections.singletonList(waveletOp)));

     }

    });

    return document;
  }


  @Override
  protected void setUp() throws Exception {

    AccountStore accountStore = new MemoryStore();
    accountStore.putAccount(new HumanAccountDataImpl(ParticipantId.ofUnsafe("alice@example.com")));
    org.eclipse.jetty.server.SessionManager jettySessionManager =
        mock(org.eclipse.jetty.server.SessionManager.class);
    sessionManager = mock(SessionManager.class);
    session = mock(HttpSession.class);
    when(session.getAttribute(SessionManager.USER_FIELD)).thenReturn(ParticipantId.ofUnsafe("alice@example.com"));

    generatedDeltas = new ArrayList<TransformedWaveletDelta>();
    opContextFactory = new WaveletOperationContext.Factory() {

      @Override
      public WaveletOperationContext createContext() {
        return createContext(currentParticipantId);
      }

      @Override
      public WaveletOperationContext createContext(ParticipantId creator) {
        return new WaveletOperationContext(creator, System.currentTimeMillis(), 1L, HashedVersion.unsigned(0L));
      }


    };

    ParticipantId alice = ParticipantId.ofUnsafe("alice@example.org");
    ParticipantId bob = ParticipantId.ofUnsafe("bob@example.org");
    ParticipantId chris = ParticipantId.ofUnsafe("chris@example.org");

    currentParticipantId = alice;
    document = createStubDocument(generatedDeltas, opContextFactory, "Hello World");
    document.appendXml(XmlStringBuilder.createText("Hello world"));

    String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua";
    String[] parts = text.split(" ");

    // Write the text with different users

    int c = 0;
    int location = 6;
    for (String part: parts) {

      int mod = c % 3;
      switch (mod) {
        case 0: currentParticipantId = alice;
                break;
        case 1: currentParticipantId = bob;
                break;
        case 2: currentParticipantId = chris;
                break;
      }

      document.insertText(location, part+" ");
      location += part.length()+1;
      c++;

    }

    waveletProvider = new FakeWaveletProvider(generatedDeltas);
    servlet = new ContributionsServlet(waveletProvider, sessionManager);

  }


  public void testBadCredentials() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(null);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(sessionManager.getLoggedInUser(request)).thenReturn(null);

    servlet.doGet(request, response);
    verify(response, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN);

  }


  public void testBadWaveRef() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getSession()).thenReturn(session);
    when(sessionManager.getLoggedInUser(request)).thenReturn(ParticipantId.ofUnsafe("alice@example.org"));

    // when(request.getPathInfo()).thenReturn("/example.org/w+12345/example.com/data+model/b+12345");
    when(request.getPathInfo()).thenReturn("/xxxxxx//b+12345");

    servlet.doGet(request, response);
    verify(response, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST);
  }

  public void testNoWaveletVersion() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getSession()).thenReturn(session);
    when(sessionManager.getLoggedInUser(request)).thenReturn(ParticipantId.ofUnsafe("alice@example.org"));

    when(request.getPathInfo()).thenReturn("/example.org/w+12345/example.com/data+model/b+12345");

    servlet.doGet(request, response);
    verify(response, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST);
  }

  public void testBadWaveletVersion() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getSession()).thenReturn(session);
    when(sessionManager.getLoggedInUser(request)).thenReturn(ParticipantId.ofUnsafe("alice@example.org"));

    when(request.getPathInfo()).thenReturn("/example.org/w+12345/example.com/data+model/b+12345/BASE64/xxx");

    servlet.doGet(request, response);
    verify(response, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST);
  }

  public void testBadWaveletHashedVersion() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getSession()).thenReturn(session);
    when(sessionManager.getLoggedInUser(request)).thenReturn(ParticipantId.ofUnsafe("alice@example.org"));

    when(request.getPathInfo()).thenReturn("/example.org/w+12345/example.com/data+model/b+12345/@@@@/99");

    servlet.doGet(request, response);
    verify(response, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST);
  }

  public void testResponseOk() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getSession()).thenReturn(session);
    when(sessionManager.getLoggedInUser(request)).thenReturn(ParticipantId.ofUnsafe("alice@example.org"));

    // The blip id matters!!!
    when(request.getPathInfo()).thenReturn("/example.org/w+12345/example.com/data+model/b+dummy/BASE64/99");

    StringWriter writer = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(writer));

    servlet.doGet(request, response);
    verify(response, times(1)).getWriter();
  }

}
