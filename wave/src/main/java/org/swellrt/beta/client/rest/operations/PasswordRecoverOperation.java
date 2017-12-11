package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.CredentialData;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.common.SException;

import com.google.gwt.safehtml.shared.UriUtils;

public final class PasswordRecoverOperation
    extends ServerOperation<CredentialData, Void> {


  public PasswordRecoverOperation(ServiceContext context, CredentialData options,
      ServiceOperation.Callback<Void> callback) {
    super(context, options, callback);
  }


  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  protected void buildRestParams() throws SException {

    addPathElement("email");

    if (options.getId() != null)
      addQueryParam("id-or-email", options.getId());
    else if (options.getEmail() != null)
      addQueryParam("id-or-email", UriUtils.encode(options.getEmail()));
    else
      throw new SException(SException.MISSING_PARAMETERS, null, "User id or email is required");

    addQueryParam("recover-url", options.getUrl());
    addQueryParam("method", "password-reset");

  }

}
