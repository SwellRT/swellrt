package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.ServiceContext.ObjectCallback;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.client.operation.OperationException;
import org.swellrt.beta.model.SObject;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;

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
    
    @JsProperty
    public SObject getObject();
    
  }
  
  private final ServiceContext context;
  
  public OpenOperation(ServiceContext context) {
    this.context = context;
  }
  

  @Override
  public void execute(Options options, Callback<Response> callback) throws OperationException {

    try {
            
      if (!context.isSession())
        throw new OperationException("Session not started");
      
      WaveId waveId = null;
      if (options.getId() != null) {
        waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(options.getId());
      } else {
        waveId = context.generateWaveId();
      }

      context.getObject(waveId, new ObjectCallback() {

        @Override
        public void onReady(SObject object) {
          callback.onSuccess(new OpenOperation.Response() {
            @Override
            public SObject getObject() {
              return object;
            }           
          });         
        }

        @Override
        public void onFailure(Exception e) {
          callback.onError(new OperationException("Error retrieving object", e));
        }
        
      });
      

    } catch (InvalidIdException e) {
      callback.onError(new OperationException("Object id invalid syntax", e));
    }

  }

}
