package org.swellrt.server.box.servlet;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.swellrt.server.box.servlet.AccountService.ServiceData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountAttachmentStore;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AccountServiceTest extends TestCase {

  private final static String IMAGE_BASE64 =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABoAAAAZCAYAAAAv3j5gAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAQZJREFUeNrsle0NgyAQhqHpf92gjuAIHcFuwAgdoSN0BN3AETqCI3QEN7hy5kguiHgF2vRHL3kT0YPnPgAVAKhSstZaPULfjqqQaa0XiFUVdCiYyYzLkbqVzwcgQOO2GGgDwmEmG2StiUC4DPkvg9GqTsgGhDI44cpTxUgji9fkf6NnI4bRAuMb0TlNDBzzwwQ6HilkwHr2DoN++i1xIJMI4rA7ivfQK7t498TUCzbPcm1ArvZAmhyzzS6mY98P6ku2dXtfqG9nOpioU9btHijdYMtgAr+BhkFdAJW0dCpwuOqUK2jP3+8RnoNZ2PwpuXSS9L1yws/tuj8oCzS485MwXzz3JcAARxWLH4IZZWIAAAAASUVORK5CYII=";


  AccountStore accountStore;
  AccountAttachmentStore accountAttachmentStore;
  AccountService service;
  SessionManager sessionManager;

  protected void setUp() throws Exception {

    accountStore = new MemoryStore();
    accountAttachmentStore = mock(AccountAttachmentStore.class);
    when(
        accountAttachmentStore.storeAvatar((ParticipantId) anyObject(), anyString(), anyString(),
            anyString())).thenReturn("image/png;image.png");

    sessionManager = mock(SessionManager.class);

    service =
        new AccountService(sessionManager, accountStore, accountAttachmentStore, "example.com");
  }

  protected ServletInputStream asServletInputStream(final String data) {
    return new ServletInputStream() {

      ByteArrayInputStream byteInputStream =
          new ByteArrayInputStream(data.getBytes(Charset.forName("UTF-8")));

      int b;

      @Override
      public boolean isFinished() {
        return b == -1;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
      }

      @Override
      public int read() throws IOException {
        this.b = byteInputStream.read();
        return b;
      }

    };
  }

  public void executeService(String path, String method, ServiceData requestData,
      ByteArrayOutputStream responseStream, int expectedHttpResponseCode) throws IOException {

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    when(request.getPathInfo()).thenReturn(path);
    when(request.getMethod()).thenReturn(method);

    when(request.getInputStream()).thenReturn(asServletInputStream(requestData.toJson()));
    when(response.getCharacterEncoding()).thenReturn("UTF-8");

    when(response.getWriter()).thenReturn(new PrintWriter(responseStream));

    service.execute(request, response);

    verify(response).setStatus(expectedHttpResponseCode);

  }


  public void testCreateAccount() throws IOException {

    // Mock data

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(null);

    // Test

    ServiceData requestData = new ServiceData();
    requestData.id = "joe";
    requestData.email = "joe@email.example.com";
    requestData.locale = "en_EN";
    requestData.password = "_password_";
    requestData.avatar_data = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account", "POST", requestData, responseStream, HttpServletResponse.SC_OK);

    ServiceData responseData = ServiceData.fromJson(responseStream.toString("UTF-8"));
    assertEquals(requestData.email, responseData.email);
    assertEquals(requestData.locale, responseData.locale);
    assertTrue(responseData.avatar_url.contains("image.png"));

  }

  public void testCreateAccountButAlreadyExists() throws IOException, InvalidParticipantAddress,
      PersistenceException {

    // Mock data

    ParticipantId p = ParticipantId.of("joe", "example.com");
    accountStore.putAccount(new HumanAccountDataImpl(p));

    // Test

    ServiceData requestData = new ServiceData();
    requestData.id = "joe";
    requestData.email = "joe@email.example.com";
    requestData.locale = "en_EN";
    requestData.password = "_password_";
    requestData.avatar_data = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account", "POST", requestData, responseStream,
        HttpServletResponse.SC_FORBIDDEN);


  }

  public void testUpdateAccount() throws PersistenceException, InvalidParticipantAddress,
      IOException {

    // Mock data

    ParticipantId p = ParticipantId.of("joe", "example.com");

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(p);

    HumanAccountData account = new HumanAccountDataImpl(p);
    account.setAvatarFileId("image/png;old_image.png");
    account.setEmail("oldjoe@email.example.com");
    account.setLocale("es_ES");

    accountStore.putAccount(account);

    // Test

    ServiceData requestData = new ServiceData();
    requestData.email = "joe@email.example.com";
    requestData.locale = "en_EN";
    requestData.password = "_password_";
    requestData.avatar_data = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "POST", requestData, responseStream,
        HttpServletResponse.SC_OK);

    ServiceData responseData = ServiceData.fromJson(responseStream.toString("UTF-8"));
    assertEquals(requestData.email, responseData.email);
    assertEquals(requestData.locale, responseData.locale);
    assertTrue(responseData.avatar_url.contains("image.png"));


  }


  public void testUpdateAccountNotLoggedUser() throws PersistenceException,
      InvalidParticipantAddress, IOException {

    // Mock data

    ParticipantId p = ParticipantId.of("joe", "example.com");

    HumanAccountData account = new HumanAccountDataImpl(p);
    account.setAvatarFileId("image/png;old_image.png");
    account.setEmail("oldjoe@email.example.com");
    account.setLocale("es_ES");

    accountStore.putAccount(account);

    // Test

    ServiceData requestData = new ServiceData();
    requestData.email = "joe@email.example.com";
    requestData.locale = "en_EN";
    requestData.password = "_password_";
    requestData.avatar_data = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "POST", requestData, responseStream,
        HttpServletResponse.SC_FORBIDDEN);

  }

  public void testGetAccount() throws InvalidParticipantAddress, PersistenceException, IOException {

    // Mock data

    ParticipantId p = ParticipantId.of("joe", "example.com");

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(p);

    HumanAccountData account = new HumanAccountDataImpl(p);
    account.setAvatarFileId("image/png;image.png");
    account.setEmail("joe@email.example.com");
    account.setLocale("en_EN");

    accountStore.putAccount(account);

    // Test

    ServiceData requestData = new ServiceData();

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "GET", requestData, responseStream,
        HttpServletResponse.SC_OK);

    ServiceData responseData = ServiceData.fromJson(responseStream.toString("UTF-8"));
    assertEquals("joe@email.example.com", responseData.email);
    assertEquals("en_EN", responseData.locale);
    assertTrue(responseData.avatar_url.contains("image.png"));

  }

  public void testGetAccountNotLoggedUser() throws InvalidParticipantAddress, PersistenceException,
      IOException {

    // Mock data

    ParticipantId p = ParticipantId.of("joe", "example.com");

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(p);

    HumanAccountData account = new HumanAccountDataImpl(p);
    account.setAvatarFileId("image/png;image.png");
    account.setEmail("joe@email.example.com");
    account.setLocale("en_EN");

    accountStore.putAccount(account);

    // Test

    ServiceData requestData = new ServiceData();

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "GET", requestData, responseStream,
        HttpServletResponse.SC_OK);

    ServiceData responseData = ServiceData.fromJson(responseStream.toString("UTF-8"));
    assertEquals("joe@email.example.com", responseData.email);
    assertEquals("en_EN", responseData.locale);
    assertTrue(responseData.avatar_url.contains("image.png"));

  }

  public void testGetAccountAvatar() {

  }

}
