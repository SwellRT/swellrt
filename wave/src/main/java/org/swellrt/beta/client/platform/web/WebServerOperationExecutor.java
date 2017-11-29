package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServerOperation.Method;
import org.swellrt.beta.client.rest.ServerOperationExecutor;
import org.swellrt.beta.client.rest.ServiceOperation.OperationError;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;

public class WebServerOperationExecutor<O extends ServerOperation.Options, R extends ServerOperation.Response>
    extends ServerOperationExecutor<O, R>
{

  protected WebServerOperationExecutor(ServiceContext context) {
    super(context);
  }

  private RequestBuilder.Method toMethod(ServerOperation.Method method) {
    switch (method) {
    case DELETE:
      return RequestBuilder.DELETE;
    case GET:
      return RequestBuilder.GET;
    case POST:
      return RequestBuilder.POST;
    default:
      return RequestBuilder.GET;
    }
  }

  @Override
  protected void executeHTTP(Method method, String url,
      ServerOperationExecutor.Header[] headers, String body,
      ServerOperationExecutor.HTTPCallback httpCallback) throws Exception {

    RequestBuilder rb = new RequestBuilder(toMethod(method), url);
    rb.setIncludeCredentials(true);

    for (int i = 0; i < headers.length; i++) {
      Header header = headers[i];
      if (header.value != null)
        rb.setHeader(header.name, header.value);
    }

    try {

      rb.sendRequest(body, new RequestCallback() {

        @Override
        public void onError(Request request, Throwable exception) {
          httpCallback.onFailure(exception);
        }

        @Override
        public void onResponseReceived(Request request,
            com.google.gwt.http.client.Response response) {
          httpCallback.onResponse(response.getStatusCode(), response.getStatusText(), response.getText());
        }

      });

    } catch (RequestException e) {
      httpCallback.onFailure(e);

    } catch (RuntimeException e) {
      httpCallback.onFailure(e);
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  protected R parseResponse(String json) {
    return (R) JsonUtils.safeEval(json);
  }

  @Override
  protected OperationError parseServiceError(String json) {
    return (OperationError) JsonUtils.safeEval(json);
  }

  @Override
  protected String toJson(O options) {
    if (options != null) {
      if (options instanceof JavaScriptObject) {
        return JsonUtils.stringify((JavaScriptObject) options);
      }
    }
    return null;
  }

}
