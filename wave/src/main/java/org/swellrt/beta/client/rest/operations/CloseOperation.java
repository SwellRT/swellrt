package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServiceOperation;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class CloseOperation
    extends ServiceOperation<CloseOperation.Options, CloseOperation.Response> {

  public CloseOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
    // TODO Auto-generated constructor stub
  }

  @JsType(isNative = true)
  public interface Options extends ServiceOperation.Options {

    @JsProperty
    public String getId();
  }

  @JsType
  public interface Response extends ServiceOperation.Response {

  }


}
