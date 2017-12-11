package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.ObjectId;
import org.swellrt.beta.model.SObject;

public final class OpenOperation extends ServiceOperation<ObjectId, SObject> {


  public OpenOperation(ServiceContext context, ObjectId options,
      ServiceOperation.Callback<SObject> callback) {
    super(context, options, callback);
  }

}
