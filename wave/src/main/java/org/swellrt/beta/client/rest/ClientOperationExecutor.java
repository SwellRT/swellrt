package org.swellrt.beta.client.rest;

import org.swellrt.beta.client.rest.operations.CloseOperation;
import org.swellrt.beta.client.rest.operations.OpenOperation;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;

import com.google.common.util.concurrent.FutureCallback;

public class ClientOperationExecutor extends OperationExecutor {

  @Override
  public void execute(
      ServiceOperation<? extends ServiceOperation.Options, ? extends ServiceOperation.Response> operation) {
    throw new IllegalStateException("Can't execute generic ServiceOperation instance");
  }

  public void execute(OpenOperation operation) {

    try {

      if (!operation.context.hasSession()) {
        operation.doFailure(new SException(ResponseCode.NOT_LOGGED_IN));
      }

      WaveId waveId = null;
      String id = operation.options.getId();
      // Wave domain part is optional
      if (id != null) {
        if (!id.contains("/")) {
          id = operation.context.getServiceSession().getWaveDomain() + "/" + id;
        }
        waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(id);
      } else {
        waveId = operation.context.generateWaveId(operation.options.getPrefix());
      }

      operation.context.getObject(waveId, new FutureCallback<SWaveObject>() {

        @Override
        public void onSuccess(SWaveObject object) {
          operation.doSuccess(object);
        }

        @Override
        public void onFailure(Throwable e) {
          operation.doFailure(e);
        }

      });

    } catch (InvalidIdException e) {
      operation.doFailure(new SException(ResponseCode.INVALID_ID));
    }

  }

  public void execute(CloseOperation operation) {

    try {
      WaveId waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(operation.options.getId());
      operation.context.closeObject(waveId);
    } catch (InvalidIdException e) {
      operation.doFailure(new SException(SException.BAD_REQUEST, e, "Object id is not valid"));
    } catch (SException e) {
      operation.doFailure(e);
    }

    operation.doSuccess(new Void() {
    });
  }


}
