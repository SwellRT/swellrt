package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public final class ResumeOperation
    extends ServerOperation<ResumeOperation.Options, ResumeOperation.Response> {


  @JsType(isNative = true)
  public static class Options implements ServiceOperation.Options {

    public String id;

    @JsOverlay
    public final String getId() {
      return id;
    }

  }

  @JsType(isNative = true)
  public static class Response extends AccountDataResponse implements ServerOperation.Response {
  }


  public ResumeOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  protected void buildRestParams() throws SException {
    addPathElement("auth");
    addPathElement(options.getId());
  }

}
