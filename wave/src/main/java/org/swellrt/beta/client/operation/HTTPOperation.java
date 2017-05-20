package org.swellrt.beta.client.operation;

import org.swellrt.beta.client.ServiceContext;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A base class for operations which performs HTTP requests. The underlying HTTP client is platform dependent.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 * @param <O> operation options
 * @param <R> operation callback
 */
public abstract class HTTPOperation<O  extends Operation.Options, R extends Operation.Response> implements Operation<O, R> {

  @SuppressWarnings("serial")
  public static class HTTPOperationException extends Exception {

    private final int statusCode;
    private final String statusMessage;

    public int getStatusCode() {
      return statusCode;
    }

    public String getStatusMessage() {
      return statusMessage;
    }

    public HTTPOperationException(int statusCode, String statusMessage) {
      this.statusCode = statusCode;
      this.statusMessage = statusMessage;
    }

  }

  @JsType(isNative = true)
  private interface ResponseError {

    @JsProperty
    public String getError();
  }
  private final static String HEADER_WINDOW_ID = "X-window-id";

  private final static String PARAM_URL_SESSION_ID = "sid";
  private final static String PARAM_URL_TRANSIENT_SESSION_ID = "tid";

  private final static int STATUS_READY = 0;
  private final static int STATUS_IN_PROGRESS = 1;
  private final static int STATUS_COMPLETED = 2;

  private final static String querySeparator = "?";
  private final static String queryAmp = "&";
  private final static String queryEquals = "=";
  private final static String pathSeparator = "/";
  private final static String requestContext = "swell";

  private final static String headerContentType = "Content-Type";
  private final static String headerContentTypeValue = "text/plain; charset=utf-8";

  private String path = requestContext;
  private String query = "";
  private String body = null;

  private int status = STATUS_READY;

  private final ServiceContext context;
  private Callback<R> callback;

  private boolean sessionInURL= true;

  protected HTTPOperation(ServiceContext context) {
    this.context = context;
  }

  /**
   * Set to false to not propagate session id in URLs never (no URL rewriting).
   * By the default this this flag is true: if session can't be sent as
   * cookie, URL rewriting is done.
   * @param state
   */
  protected void setSessionInURLFlag(boolean state) {
    sessionInURL = state;
  }

  /**
   * The path's context, with no "/" at the beginning
   * @param context
   * @return
   */
  protected HTTPOperation<O, R> setPathContext(String context) {
    //Preconditions.checkArgument(element != null && !element.isEmpty(), "Empty string");
    if (context.startsWith(pathSeparator))
      context = context.substring(1);
    path = context;
    return this;
  }

  protected HTTPOperation<O, R> addPathElement(String element) {
    //Preconditions.checkArgument(element != null && !element.isEmpty(), "Empty string");
    path += pathSeparator+element;
    return this;
  }

  protected HTTPOperation<O, R> addQueryParam(String param, String value) {
    if (query.isEmpty())
      query += querySeparator;
    else
      query += queryAmp;

    // TODO encode params
    query += param + queryEquals + value;

    return this;

  }

  protected HTTPOperation<O, R>  setBody(String body) {
    this.body = body;
    return this;
  }

  private RequestBuilder getRequest(Method method, String url) {

    RequestBuilder rb = new RequestBuilder(method, url);
    rb.setIncludeCredentials(true);
    rb.setHeader(headerContentType, headerContentTypeValue);

    String windowId = getServiceContext().getWindowId();
    if (windowId != null)
      rb.setHeader(HEADER_WINDOW_ID, windowId);

    return rb;
  }

  private String buildUrl() {

    String url = getServiceContext().getHTTPAddress();
    if (!url.endsWith(pathSeparator))
      url += pathSeparator;

    url += path;

    if (sessionInURL &&
        !getServiceContext().isSessionCookieAvailable()) {

      if (getServiceContext().getTransientSessionId() != null)
        url += ";" + PARAM_URL_TRANSIENT_SESSION_ID + "=" + getServiceContext().getTransientSessionId();

      if (getServiceContext().getSessionId() != null)
        url += ";" + PARAM_URL_SESSION_ID + "=" + getServiceContext().getSessionId();
    }

    url += query;

    return url;
  }


  private void executeHttp(Method method) {
    //Preconditions.checkArgument(status == STATUS_READY, "HTTP operations only can be executed once");
    status = STATUS_IN_PROGRESS;
    RequestBuilder requestBuilder = getRequest(method, buildUrl());
    try {

      requestBuilder.sendRequest(body, new RequestCallback() {

        @Override
        public void onError(Request request, Throwable exception) {
          // TODO filter or wrap exception
          HTTPOperation.this.status = STATUS_COMPLETED;
          HTTPOperation.this.onError(exception, callback);
        }

        @Override
        public void onResponseReceived(Request request,
            com.google.gwt.http.client.Response response) {
          HTTPOperation.this.status = STATUS_COMPLETED;

          if (response.getStatusCode() != 200) {
            String statusMessage = response.getStatusText();
            try {
              ResponseError e = JsonUtils.safeEval(response.getText());
              statusMessage = e.getError();
            } catch (IllegalArgumentException ex) {

            }
            HTTPOperation.this.onError(new HTTPOperationException(response.getStatusCode(), statusMessage), callback);
          } else {
            HTTPOperation.this.onSuccess(response.getStatusCode(), response.getText(), callback);
          }
        }
      });

    } catch (RequestException e) {
        // TODO filter or wrap exception
        HTTPOperation.this.status = STATUS_COMPLETED;
        HTTPOperation.this.onError(e, callback);

    } catch (RuntimeException e) {
      // TODO filter or wrap exception
      HTTPOperation.this.status = STATUS_COMPLETED;
      HTTPOperation.this.onError(e, callback);
  }
  }

  protected void executePost(Callback<R> callback) {
    this.callback = callback;
    executeHttp(RequestBuilder.POST);
  }

  protected void executeGet(Callback<R> callback) {
    this.callback = callback;
    executeHttp(RequestBuilder.GET);
  }

  protected void executeDelete(Callback<R> callback) {
    this.callback = callback;
    executeHttp(RequestBuilder.DELETE);
  }

  protected ServiceContext getServiceContext() {
    return context;
  }

  protected abstract void onError(Throwable exception, Callback<R> callback);

  protected abstract void onSuccess(int statusCode, String data, Callback<R> callback);

  protected String generateBody(O options) {
    if (options != null) {
      if (options instanceof JavaScriptObject) {
        return JsonUtils.stringify((JavaScriptObject) options);
      }
    }
    return "Options couldn't be serialized to JSON";
  }

  @SuppressWarnings("unchecked")
  protected R generateResponse(String json) {
    return (R) JsonUtils.safeEval(json);
  }

  @Override
  public abstract void execute(O options, Callback<R> callback);


}
