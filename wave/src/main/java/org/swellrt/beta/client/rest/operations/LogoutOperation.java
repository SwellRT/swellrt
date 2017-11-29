package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class LogoutOperation
    extends ServerOperation<LogoutOperation.Options, LogoutOperation.Response> {

  public LogoutOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<Response> callback) {
    super(context, options, callback);
  }

  @JsType(isNative = true)
  public interface Options extends ServerOperation.Options {

    @JsProperty
    public String getId();

  }

  @JsType(isNative = true)
  public interface Response extends ServerOperation.Response {


  }

  private boolean resetContext = false;


  @Override
  public void doSuccess(LogoutOperation.Response response) {

    // reset the context after http call to send cookies
    if (resetContext)
      getContext().reset();

    super.doSuccess(response);
  }



  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.DELETE;
  }

  @Override
  public void buildRestParams() throws SException {

    addPathElement("auth");

    if (getContext().isSession()) {

      if (getOptions() == null || getOptions().getId() == null || (getOptions().getId() != null
          && getOptions().getId().equals(getContext().getParticipantId()))) {
        addPathElement(getContext().getParticipantId());
        resetContext = true;
        }
    } else if (getOptions().getId() != null) {
      addPathElement(getOptions().getId());
    } else {
      doFailure(new SException(SException.MISSING_PARAMETERS, null, "Missing user parameter"));
    }

  }



}
