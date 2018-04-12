package org.swellrt.beta.client.rest;

import java.util.HashMap;
import java.util.Map;

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

  protected interface HTTPCallback {

    void onResponse(int statusCode, String statusText, String response);

    void onFailure(Throwable exception);

  }

  private final ServiceContext context;

  private Map<String, String> headerMap = new HashMap<String, String>();

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

  private Map<String, String> getHeaders(
      ServerOperation<? extends ServiceOperation.Options, ? extends ServiceOperation.Response> operation) {

    headerMap.clear();

    String windowId = ServiceSession.getWindowId();
    if (windowId != null)
      headerMap.put(HEADER_WINDOW_ID, windowId);

    // default headers
    headerMap.put("Content-Type", "application/json; charset=utf-8");
    headerMap.put("Accept", "text/plain, application/json");

    // overwrite with custom headers
    headerMap.putAll(operation.getHeaders());

    return headerMap;
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

      operation.validateOptions();

      String body = null;
      if (operation.sendOptionsAsBody()) {
        body = toJson(operation.options);
      }

      executeHTTP(operation.getMethod(),
          buildUrl(operation.getRestContext(), operation.getRestParams(),
              operation.sendSessionInUrl()),
          getHeaders(operation),
          body, new HTTPCallback() {

            @Override
            public void onResponse(int statusCode, String statusText, String response) {

              if (statusCode == 200) {

                operation.doSuccessRaw(response);

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

    } catch (Exception e) {
      operation.doFailure(e);
    }

  }

  protected abstract void executeHTTP(Method method, String url, Map<String, String> headers,
      String body,
      HTTPCallback httpCallback) throws Exception;

  protected abstract OperationError parseServiceError(String json);

  protected abstract <O extends ServiceOperation.Options> String toJson(O options);

}
