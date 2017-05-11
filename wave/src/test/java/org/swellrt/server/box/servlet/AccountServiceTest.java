package org.swellrt.server.box.servlet;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.swellrt.server.box.servlet.AccountService.AccountServiceData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountAttachmentStore;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

public class AccountServiceTest extends TestCase {

  private final static String IMAGE_BASE64 =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABoAAAAZCAYAAAAv3j5gAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAQZJREFUeNrsle0NgyAQhqHpf92gjuAIHcFuwAgdoSN0BN3AETqCI3QEN7hy5kguiHgF2vRHL3kT0YPnPgAVAKhSstZaPULfjqqQaa0XiFUVdCiYyYzLkbqVzwcgQOO2GGgDwmEmG2StiUC4DPkvg9GqTsgGhDI44cpTxUgji9fkf6NnI4bRAuMb0TlNDBzzwwQ6HilkwHr2DoN++i1xIJMI4rA7ivfQK7t498TUCzbPcm1ArvZAmhyzzS6mY98P6ku2dXtfqG9nOpioU9btHijdYMtgAr+BhkFdAJW0dCpwuOqUK2jP3+8RnoNZ2PwpuXSS9L1yws/tuj8oCzS485MwXzz3JcAARxWLH4IZZWIAAAAASUVORK5CYII=";


  protected static HumanAccountData createHumanAccount(ParticipantId pid, String email,
      String avatarFileId, String locale) {
    HumanAccountData account = new HumanAccountDataImpl(pid);
    account.setAvatarFileId(avatarFileId);
    account.setEmail(email);
    account.setLocale(locale);
    return account;
  }

  protected static HumanAccountData ACCOUNT_JOE = createHumanAccount(
      ParticipantId.ofUnsafe("joe@example.com"), "joe@mail.example.com", "image/png;image.png",
      "en_GB");

  protected static HumanAccountData ACCOUNT_TOM = createHumanAccount(
      ParticipantId.ofUnsafe("tom@example.com"), "tom@mailexample.com", "image/png;image.png",
      "en_US");

  protected static HumanAccountData ACCOUNT_MAT = createHumanAccount(
      ParticipantId.ofUnsafe("mat@example.com"), "mat@mail.example.com", "image/png;image.png",
      "es_ES");

  AccountStore accountStore;
  AccountAttachmentStore accountAttachmentStore;
  AccountService service;
  SessionManager sessionManager;


  @Override
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

  public void executeService(String path, String method, AccountServiceData requestData,
      ByteArrayOutputStream responseStream, int expectedHttpResponseCode) throws IOException {

    executeService(path, method, requestData, responseStream, expectedHttpResponseCode, null);

  }

  public void executeService(String path, String method, AccountServiceData requestData,
      ByteArrayOutputStream responseStream, int expectedHttpResponseCode,
      Map<String, String> parameters) throws IOException {

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:9898"));
    HttpServletResponse response = mock(HttpServletResponse.class);

    when(request.getPathInfo()).thenReturn(path);
    when(request.getMethod()).thenReturn(method);

    if (parameters != null) {
      for (Entry<String, String> p : parameters.entrySet()) {
        when(request.getParameter(eq(p.getKey()))).thenReturn(p.getValue());
      }
    }

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

    AccountServiceData requestData = new AccountServiceData();
    requestData.id = "joe";
    requestData.email = "joe@email.example.com";
    requestData.locale = "en_EN";
    requestData.password = "_password_";
    requestData.avatarData = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account", "POST", requestData, responseStream, HttpServletResponse.SC_OK);

    AccountServiceData responseData =
        (AccountServiceData) ServiceData.fromJson(responseStream.toString("UTF-8"),
            AccountServiceData.class);
    assertEquals(requestData.email, responseData.email);
    assertEquals(requestData.locale, responseData.locale);
    assertTrue(responseData.avatarUrl.contains("image.png"));

  }

  public void testCreateAccountButAlreadyExists() throws IOException, InvalidParticipantAddress,
      PersistenceException {

    // Mock data

    accountStore.putAccount(ACCOUNT_JOE);

    // Test

    AccountServiceData requestData = new AccountServiceData();
    requestData.id = "joe";
    requestData.email = "joe@example.com";
    requestData.locale = "en_GB";
    requestData.password = "_password_";
    requestData.avatarData = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account", "POST", requestData, responseStream,
        HttpServletResponse.SC_FORBIDDEN);


  }

  public void testUpdateAccount() throws PersistenceException, InvalidParticipantAddress,
      IOException {

    // Mock data
    accountStore.putAccount(ACCOUNT_JOE);

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(ACCOUNT_JOE.getId());
    when(sessionManager.getLoggedInUser((HttpServletRequest) anyObject())).thenReturn(
        ACCOUNT_JOE.getId());

    // Test

    AccountServiceData requestData = new AccountServiceData();
    requestData.email = "joe@email.example.com";
    requestData.locale = "es_ES";
    requestData.password = "_password_";
    requestData.avatarData = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "POST", requestData, responseStream,
        HttpServletResponse.SC_OK);

    AccountServiceData responseData =
        (AccountServiceData) ServiceData.fromJson(responseStream.toString("UTF-8"),
            AccountServiceData.class);
    assertEquals(requestData.email, responseData.email);
    assertEquals(requestData.locale, responseData.locale);
    assertTrue(responseData.avatarUrl.contains("image.png"));


  }


  public void testUpdateAccountNotLoggedUser() throws PersistenceException,
      InvalidParticipantAddress, IOException {

    // Mock data

    accountStore.putAccount(ACCOUNT_JOE);

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(null);


    // Test

    AccountServiceData requestData = new AccountServiceData();
    requestData.email = "joe@email.example.com";
    requestData.locale = "es_ES";
    requestData.password = "_password_";
    requestData.avatarData = IMAGE_BASE64;

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "POST", requestData, responseStream,
        HttpServletResponse.SC_FORBIDDEN);

  }

  public void testGetAccount() throws InvalidParticipantAddress, PersistenceException, IOException {

    // Mock data

    accountStore.putAccount(ACCOUNT_JOE);

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(ACCOUNT_JOE.getId());
    when(sessionManager.getLoggedInUser((HttpServletRequest) anyObject())).thenReturn(
        ACCOUNT_JOE.getId());

    // Test

    AccountServiceData requestData = new AccountServiceData();

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "GET", requestData, responseStream,
        HttpServletResponse.SC_OK);

    AccountServiceData responseData =
        (AccountServiceData) ServiceData.fromJson(responseStream.toString("UTF-8"),
            AccountServiceData.class);
    assertEquals(ACCOUNT_JOE.getEmail(), responseData.email);
    assertEquals(ACCOUNT_JOE.getLocale(), responseData.locale);
    assertTrue(responseData.avatarUrl.contains("image.png"));

  }

  public void testGetAccountNotLoggedUser() throws InvalidParticipantAddress, PersistenceException,
      IOException {

    // Mock data

    accountStore.putAccount(ACCOUNT_JOE);

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(ACCOUNT_MAT.getId());
    when(sessionManager.getLoggedInUser((HttpServletRequest) anyObject())).thenReturn(
        ACCOUNT_MAT.getId());

    // Test

    AccountServiceData requestData = new AccountServiceData();

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account/joe", "GET", requestData, responseStream,
        HttpServletResponse.SC_OK);

    AccountServiceData responseData =
        (AccountServiceData) ServiceData.fromJson(responseStream.toString("UTF-8"),
            AccountServiceData.class);
    assertNull(responseData.email);
    assertEquals(ACCOUNT_JOE.getLocale(), responseData.locale);
    assertTrue(responseData.avatarUrl.contains("image.png"));

  }

  public void testGetAccountAvatar() {

  }


  public void testQueryAccounts() throws PersistenceException, IOException {

    // Mock data

    accountStore.putAccount(ACCOUNT_JOE);
    accountStore.putAccount(ACCOUNT_TOM);
    accountStore.putAccount(ACCOUNT_MAT);

    when(sessionManager.getLoggedInUser((HttpSession) anyObject())).thenReturn(ACCOUNT_MAT.getId());
    when(sessionManager.getLoggedInUser((HttpServletRequest) anyObject())).thenReturn(
        ACCOUNT_MAT.getId());

    when(sessionManager.getAllLoggedInUser((HttpServletRequest) anyObject())).thenReturn(CollectionUtils.immutableSet(ACCOUNT_MAT.getId()));

    // Test
    AccountServiceData requestData = new AccountServiceData();

    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    executeService("/account", "GET", requestData, responseStream, HttpServletResponse.SC_OK,
        ImmutableMap.<String, String> of("p",
            "joe@example.com;tom@example.com;mike@example.com;mat@example.com"));

    AccountServiceData[] responseData =
        (AccountServiceData[]) ServiceData.arrayFromJson(responseStream.toString("UTF-8"),
            AccountServiceData[].class);

    assertEquals(3, responseData.length);
    // We expect results in the same order as query
    assertEqualsAccount(ACCOUNT_JOE, responseData[0]);
    assertEqualsAccount(ACCOUNT_TOM, responseData[1]);
    assertEqualsAccount(ACCOUNT_MAT, responseData[2]);
  }

  protected void assertEqualsAccount(HumanAccountData expected, AccountServiceData actual) {
    assertEquals(expected.getId().getAddress(), actual.id);
  }


}
