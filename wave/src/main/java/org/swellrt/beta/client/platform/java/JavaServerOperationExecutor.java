package org.swellrt.beta.client.platform.java;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation.Method;
import org.swellrt.beta.client.rest.ServerOperationExecutor;
import org.swellrt.beta.client.rest.ServiceOperation.OperationError;
import org.swellrt.beta.client.rest.ServiceOperation.Options;

import com.google.gson.Gson;

public class JavaServerOperationExecutor extends ServerOperationExecutor {

  CloseableHttpClient hc = HttpClients.createDefault();
  Gson gson = new Gson();

  protected JavaServerOperationExecutor(ServiceContext context) {
    super(context);
  }


  @SuppressWarnings("unchecked")
  @Override
  protected void executeHTTP(Method method, String url, Map<String, String> headers, String body,
      HTTPCallback httpCallback) throws Exception {

    HttpUriRequest hm = null;

    switch (method) {

    case GET:
      hm = new HttpGet(url);
        break;

    case POST:
      HttpPost post = new HttpPost(url);
      post.setEntity(new StringEntity(body));
      hm = post;
        break;

    case DELETE:
      hm = new HttpDelete(url);
        break;

    default:
      httpCallback.onFailure(new IllegalStateException("HTTP method not implemented"));
      return;
    }

    final HttpUriRequest req = hm;
    headers.forEach((name, value) -> {
      req.addHeader(name, value);
    });


    try {

      hc.execute(hm, new ResponseHandler<String>() {

        @Override
        public String handleResponse(HttpResponse response)
            throws ClientProtocolException, IOException {

          int status = response.getStatusLine().getStatusCode();
          HttpEntity entity = response.getEntity();

          String result = entity != null ? EntityUtils.toString(entity, Charset.forName("UTF-8"))
                : "";

          httpCallback.onResponse(status, response.getStatusLine().getReasonPhrase(), result);
          return result;

        }

      });

    } catch (ClientProtocolException e) {
      httpCallback.onFailure(e);
    } catch (IOException e) {
      httpCallback.onFailure(e);
    }

  }


  @Override
  protected OperationError parseServiceError(String json) {
    return gson.fromJson(json, OperationError.class);
  }


  @Override
  protected <O extends Options> String toJson(O options) {
    if (options != null)
      return gson.toJson(options);

    return null;
  }

}
