package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.common.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class CloseOperation implements Operation<CloseOperation.Options, CloseOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();
  }

  @JsType
  public interface Response extends Operation.Response {

  }

  private final ServiceContext context;

  public CloseOperation(ServiceContext context) {
    this.context = context;
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    try {
      WaveId waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(options.getId());
      this.context.closeObject(waveId);
    } catch (InvalidIdException e) {
      if (callback != null)
        callback
            .onError(new SException(SException.BAD_REQUEST, e, "Object id is not valid"));
    } catch (SException e) {
      if (callback != null)
        callback.onError(e);
    }

    if (callback != null) {
      callback.onSuccess(null);
    }

  }



}
