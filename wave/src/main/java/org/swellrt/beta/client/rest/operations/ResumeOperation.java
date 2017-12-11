package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.AccountImpl;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.common.SException;

public final class ResumeOperation
    extends ServerOperation<Credential, Account> {


  public ResumeOperation(ServiceContext context, Credential options,
      ServiceOperation.Callback<Account> callback) {
    super(context, options, callback, AccountImpl.class);
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
