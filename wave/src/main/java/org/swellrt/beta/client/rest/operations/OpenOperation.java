package org.swellrt.beta.client.rest.operations;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.model.SObject;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class OpenOperation extends ServiceOperation<OpenOperation.Options, SObject> {


  public OpenOperation(ServiceContext context, Options options,
      ServiceOperation.Callback<SObject> callback) {
    super(context, options, callback);
  }

  @JsType(isNative = true)
  public interface Options extends ServiceOperation.Options {

    /**
     * An alphanumeric ID for the object. If it doesn't exist an object is
     * create with this ID, otherwise the object is opened. If no ID is
     * provided, a new object is created with an auto generated ID.
     */
    @JsProperty
    public String getId();

    /**
     * A prefix for a new auto generated ID.
     */
    @JsProperty
    public String getPrefix();
  }



}
