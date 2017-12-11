package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.CredentialData;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.client.rest.operations.params.VoidImpl;
import org.swellrt.beta.common.SException;


public final class PasswordOperation
    extends ServerOperation<CredentialData, Void> {



  public PasswordOperation(ServiceContext context, CredentialData options,
      ServiceOperation.Callback<Void> callback) {
    super(context, options, callback, VoidImpl.class);
  }


  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  protected void buildRestParams() throws SException {

    addPathElement("password");
    addQueryParam("id", options.getId());

    if (options.getOldPassword() != null) {
      addQueryParam("token-or-password", options.getOldPassword());
    } else if (options.getToken() != null) {
      addQueryParam("token-or-password", options.getToken());
    } else {
      throw new SException(SException.MISSING_PARAMETERS, null,
          "Missing token or old password parameter");
    }

    addQueryParam("new-password", options.getNewPassword());

  }



}
