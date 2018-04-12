package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServerOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.AccountImpl;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

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
    super(context, options, callback, AccountImpl.class);
  }


  @Override
  protected void doSuccess(Account response) {
    context.initSession(response);
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
  public boolean sendSessionInUrl() {
    return false;
  }

  @Override
  public boolean sendOptionsAsBody() {
    return true;
  }

  @Override
  public void validateOptions() throws IllegalArgumentException {

    // request anonymous login when no id is provided.
    if (options.getId() == null || options.getId().isEmpty()) {

      // this anonymous id syntax is only for the rest service.
      options.setId(ParticipantId.ANONYMOUS_PREFIX + "anonymous");
      options.setPassword(null);

    } else {

      try {
        ParticipantId.of(options.getId());
      } catch (InvalidParticipantAddress e) {
        throw new IllegalArgumentException(e);
      }

    }

  }

}
