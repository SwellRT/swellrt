package org.swellrt.beta.client.platform.web;

import java.util.Map;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServerOperation.Method;
import org.swellrt.beta.client.rest.ServerOperationExecutor;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.ServiceOperation.OperationError;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;

public class WebServerOperationExecutor extends ServerOperationExecutor
{

  public WebServerOperationExecutor(ServiceContext context) {
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
      Map<String, String> headers, String body,
      ServerOperationExecutor.HTTPCallback httpCallback) throws Exception {

    RequestBuilder rb = new RequestBuilder(toMethod(method), url);
    rb.setIncludeCredentials(true);

    headers.forEach((name, value) -> {
      rb.setHeader(name, value);
    });

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


  @Override
  protected OperationError parseServiceError(String json) {
    return (OperationError) JsonUtils.safeEval(json);
  }

  @Override
  protected <O extends ServiceOperation.Options> String toJson(O options) {
    if (options != null) {
      if (options instanceof JavaScriptObject) {
        return JsonUtils.stringify((JavaScriptObject) options);
      }
    }
    return null;
  }

}
