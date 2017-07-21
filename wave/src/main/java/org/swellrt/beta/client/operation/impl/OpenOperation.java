package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;

import com.google.common.util.concurrent.FutureCallback;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class OpenOperation implements Operation<OpenOperation.Options, OpenOperation.Response> {


  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();
  }

  @JsType
  public interface Response extends Operation.Response {


  }

  private final ServiceContext context;

  public OpenOperation(ServiceContext context) {
    this.context = context;
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {

    try {

      if (!context.isSession()) {
    	if (callback != null)
        	callback.onError(new SException(ResponseCode.NOT_LOGGED_IN));
      }

      WaveId waveId = null;
      String id = options.getId();
      // Wave domain part is optional
      if (id != null) {
        if (!id.contains("/")) {
          id = context.getWaveDomain()+ "/" + id;
        }
        waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(id);
      } else {
        waveId = context.generateWaveId();
      }

      context.getObject(waveId, new FutureCallback<SWaveObject>() {

        @Override
        public void onSuccess(SWaveObject object) {
          callback.onSuccess(object);
        }

        @Override
        public void onFailure(Throwable e) {
          if (e instanceof SException) {
            callback.onError((SException) e);
           } else {
            callback.onError(new SException(SException.OPERATION_EXCEPTION, e));
           }

        }

      });


    } catch (InvalidIdException e) {
      callback.onError(new SException(ResponseCode.INVALID_ID));
    }

  }

}
