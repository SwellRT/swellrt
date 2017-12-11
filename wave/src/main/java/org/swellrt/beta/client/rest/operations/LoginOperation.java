package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.AccountImpl;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.client.wave.WaveFactories;
import org.swellrt.beta.common.SException;

/**
 * Log in operation
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public final class LoginOperation
    extends ServerOperation<Credential, Account> {

  public LoginOperation(ServiceContext context, Credential options,
      Callback<Account> callback) {
    super(context, options, callback);
  }


  @Override
  protected void doSuccess(Account response) {
    context.init(response);
    super.doSuccess(response);
  }

  @Override
  public ServerOperation.Method getMethod() {
    return ServerOperation.Method.POST;
  }

  @Override
  protected void buildRestParams() throws SException {
    addPathElement("auth");
  }

  @Override
  protected void doSuccessJson(String json) {
    doSuccess(
        WaveFactories.json.<Account, AccountImpl> parse(json, Account.class, AccountImpl.class));
  }

  @Override
  public boolean sendSessionInUrl() {
    return false;
  }

  @Override
  public boolean sendOptionsAsBody() {
    return true;
  }

}
