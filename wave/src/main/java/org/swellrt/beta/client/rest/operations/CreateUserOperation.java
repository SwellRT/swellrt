package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.common.SException;

public final class CreateUserOperation
    extends ServerOperation<Account, Account> {


  public CreateUserOperation(ServiceContext context, Account options,
      ServiceOperation.Callback<Account> callback) {
    super(context, options, callback);
  }


  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  public boolean sendOptionsAsBody() {
    return true;
  }

  @Override
  protected void buildRestParams() throws SException {

    if (options == null || options.getId() == null || options.getPassword() == null) {
      throw new SException(SException.MISSING_PARAMETERS);
    }


    addPathElement("account");
  }



}
