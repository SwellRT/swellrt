package org.swellrt.beta.client.rest;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.ServiceSession;
import org.swellrt.beta.client.rest.ServerOperation.Method;
import org.swellrt.beta.client.rest.ServiceOperation.OperationError;
import org.swellrt.beta.client.rest.ServiceOperation.Options;
import org.swellrt.beta.client.rest.ServiceOperation.Response;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * An abstract executor of Swell HTTP REST operations, agnostic from the HTTP
 * client and JSON parsing libraries.
 *
 * @author pablojan@gmail.com
 *
 * @param <O>
 * @param <R>
 */
public abstract class ServerOperationExecutor extends OperationExecutor {

  private final static String HEADER_WINDOW_ID = "X-window-id";
  private final static String PARAM_URL_SESSION_ID = "sid";
  private final static String PARAM_URL_TRANSIENT_SESSION_ID = "tid";

  private final static String pathSeparator = "/";

  protected static class Header {
    public String name;
    public String value;

    public Header(String name, String value) {
      super();
      this.name = name;
      this.value = value;
    }
  }

  protected interface HTTPCallback {

    void onResponse(int statusCode, String statusText, String response);

    void onFailure(Throwable exception);

  }

  private final ServiceContext context;

  private Header[] headers =
    { new Header("Content-Type", "text/plain; charset=utf-8"),
      new Header(HEADER_WINDOW_ID, null) };

  protected ServerOperationExecutor(ServiceContext context) {
    this.context = context;
  }

  private String buildUrl(String restContext, String restParams, boolean sessionInUrl) {

    if (restContext.startsWith(pathSeparator))
      restContext = restContext.substring(1);

    String path = restContext;

    String url = context.getHTTPAddress();
    if (!url.endsWith(pathSeparator))
      url += pathSeparator;

    url += path;

    url += restParams;

    if (context.hasSession()) {

      ServiceSession session = context.getServiceSession();

      if (sessionInUrl && !session.isSessionCookie()) {

        if (session.getTransientSessionId() != null)
          url += ";" + PARAM_URL_TRANSIENT_SESSION_ID + "=" + session.getTransientSessionId();

        if (session.getHttpSessionId() != null)
          url += ";" + PARAM_URL_SESSION_ID + "=" + session.getHttpSessionId();
      }

    }

    return url;

  }

  private Header[] buildHeaders() {

    String windowId = ServiceSession.getWindowId();
    if (windowId != null)
      headers[1].value = windowId;

    return headers;
  }

  @Override
  public void execute(ServiceOperation<? extends Options, ? extends Response> operation) {

    Preconditions.checkNotNull(operation, "Can't execute null service operation");
    if (operation instanceof ServerOperation)
      execute((ServerOperation<?, ?>) operation);

    throw new IllegalStateException("This executor only can execute Server Operations");
  }

  public void execute(
      ServerOperation<? extends ServiceOperation.Options, ? extends ServiceOperation.Response> operation) {

    Preconditions.checkNotNull(operation, "Can't execute null service operation");

    try {

      String body = null;
      if (operation.sendOptionsAsBody()) {
        body = toJson(operation.options);
      }

      executeHTTP(operation.getMethod(),
          buildUrl(operation.getRestContext(), operation.getRestParams(),
              operation.sendSessionInUrl()),
          buildHeaders(),
          body, new HTTPCallback() {

            @Override
            public void onResponse(int statusCode, String statusText, String response) {

              if (statusCode == 200) {

                operation.doSuccessJson(response);

              } else {

                OperationError serviceError = parseServiceError(response);
                String errorMessage = serviceError != null ? serviceError.getError() : statusText;
                operation.doFailure(new SException(statusCode, errorMessage));

              }

            }

            @Override
            public void onFailure(Throwable exception) {
              operation.doFailure(exception);
            }

          });

    } catch (SException e) {
      operation.doFailure(e);
      return;
    } catch (Exception e) {
      operation.doFailure(new SException(SException.OPERATION_EXCEPTION, e));
    }

  }

  protected abstract void executeHTTP(Method method, String url, Header[] headers, String body,
      HTTPCallback httpCallback) throws Exception;

  protected abstract OperationError parseServiceError(String json);

  protected abstract <O extends ServiceOperation.Options> String toJson(O options);

}
