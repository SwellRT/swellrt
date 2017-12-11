package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.rest.ServiceOperation;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface ObjectId extends ServiceOperation.Options {

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
