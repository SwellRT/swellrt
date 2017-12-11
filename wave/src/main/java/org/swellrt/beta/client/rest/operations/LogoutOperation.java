package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.common.SException;

public final class LogoutOperation
    extends ServerOperation<Credential, Void> {

  public LogoutOperation(ServiceContext context, Credential options,
      ServiceOperation.Callback<Void> callback) {
    super(context, options, callback);
  }


  private boolean resetContext = false;


  @Override
  public void doSuccess(Void response) {

    // reset the context after http call to send cookies
    if (resetContext)
      context.reset();

    super.doSuccess(response);
  }



  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.DELETE;
  }

  @Override
  protected void buildRestParams() throws SException {

    addPathElement("auth");

    if (context.isSession()) {

      if (options == null || options.getId() == null || (options.getId() != null
          && options.getId().equals(context.getParticipantId()))) {
        addPathElement(context.getParticipantId());
        resetContext = true;
        }
    } else if (options.getId() != null) {
      addPathElement(options.getId());
    } else {
      throw new SException(SException.MISSING_PARAMETERS, null, "Missing user parameter");
    }

  }



}
