package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.ObjectId;
import org.swellrt.beta.client.rest.operations.params.Void;

public final class CloseOperation
    extends ServiceOperation<ObjectId, Void> {

  public CloseOperation(ServiceContext context, ObjectId options,
      ServiceOperation.Callback<Void> callback) {
    super(context, options, callback);
  }



}
