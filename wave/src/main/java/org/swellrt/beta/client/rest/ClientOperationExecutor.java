package org.swellrt.beta.client.rest;

import org.swellrt.beta.client.rest.operations.CloseOperation;
import org.swellrt.beta.client.rest.operations.OpenOperation;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;

import com.google.common.util.concurrent.FutureCallback;

public class ClientOperationExecutor<O extends ServiceOperation.Options, R extends ServiceOperation.Response>
    extends OperationExecutor<O, R> {

  @Override
  public void execute(ServiceOperation<O, R> operation) {
    throw new IllegalStateException("Can't execute generic ServiceOperation instance");
  }

  public void execute(OpenOperation operation) {

    try {

      if (!operation.getContext().isSession()) {
        operation.doFailure(new SException(ResponseCode.NOT_LOGGED_IN));
      }

      WaveId waveId = null;
      String id = operation.getOptions().getId();
      // Wave domain part is optional
      if (id != null) {
        if (!id.contains("/")) {
          id = operation.getContext().getWaveDomain() + "/" + id;
        }
        waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(id);
      } else {
        waveId = operation.getContext().generateWaveId(operation.getOptions().getPrefix());
      }

      operation.getContext().getObject(waveId, new FutureCallback<SWaveObject>() {

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
      WaveId waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(operation.getOptions().getId());
      operation.getContext().closeObject(waveId);
    } catch (InvalidIdException e) {
      operation.doFailure(new SException(SException.BAD_REQUEST, e, "Object id is not valid"));
    } catch (SException e) {
      operation.doFailure(e);
    }

    operation.doSuccess(new CloseOperation.Response() {

    });
  }

}
