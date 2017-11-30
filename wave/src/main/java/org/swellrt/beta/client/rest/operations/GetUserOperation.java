package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ServerAccountData;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class GetUserOperation
    extends ServerOperation<GetUserOperation.Options, GetUserOperation.Response> {


  @JsType(isNative = true)
  public interface Options extends ServiceOperation.Options {

    @JsProperty
    public String getId();

  }

  @JsType(isNative = true)
  public interface Response extends ServiceOperation.Response, ServerAccountData {

  }

  public GetUserOperation(ServiceContext context, GetUserOperation.Options options,
      ServiceOperation.Callback<GetUserOperation.Response> callback) {
    super(context, options, callback);
  }



  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.GET;
  }


  @Override
  public void buildRestParams() throws SException {

    if (!getContext().isSession()) {
      doFailure(new SException(SException.NOT_LOGGED_IN));
      return;
    }

    addPathElement("account");
    addPathElement(getContext().getParticipantId());
  }



}
