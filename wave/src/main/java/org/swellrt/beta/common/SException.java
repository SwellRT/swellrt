package org.swellrt.beta.common;

import org.swellrt.beta.client.operation.HTTPOperation.HTTPOperationException;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A shared exception for the whole SwellRT system. It tries to homogenize error codification, 
 * setting an integer code to identify each error type.
 * <p>
 * This is a JS interoperable type. 
 * <p>
 * 
 * <br>
 * Errors or exceptions triggered from following places should be encapsulated by
 * a SException:
 * <li>Wave client components: websocket, operation channels... in particular {@link RemoteWaveViewService} and {@link LiveChannelBinder}</li>
 * <li>SwellRT data model logic, in particular {@ServiceContext}</li>
 * <li>SwellRT HTTP API, in particular instances of {@link org.swellrt.beta.client.operation.Operation}</li>
 * <p><p>
 * Code values for Wave-related errors are defined in {@link ResponseCode} class, with values between 1 and 99.<p>
 * Code values for SwellRT errors are defined here, starting at 100.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@SuppressWarnings("serial")
@JsType(namespace = "swellrt", name = "Exception")
public class SException extends Exception {
  
  // Codes from SException
  
  /** a generic exception for REST operations */
  public static final int OPERATION_EXCEPTION = 100;
  
  public static final int MISSING_PARAMETERS = 101;
  
  public static final int NOT_ATTACHED_NODE = 110;
  
  public static final int ILLEGAL_CAST = 111;
  
  public static final int READ_FORBIDDEN = 112;
  
  public static final int WRITE_FORBIDDEN = 113;

  // Codes from ResponseCode
  
  public static final int UNKNOWN = -1;

  public static final int OK = 0;

  public static final int BAD_REQUEST = 1;

  public static final int INTERNAL_ERROR = 2;

  public static final int NOT_AUTHORIZED = 3;

  public static final int VERSION_ERROR = 4;

  public static final int INVALID_OPERATION = 5;

  public static final int SCHEMA_VIOLATION = 6;

  public static final int SIZE_LIMIT_EXCEEDED = 7;

  public static final int POLICY_VIOLATION = 8;

  public static final int QUARANTINED = 9;

  public static final int TOO_OLD = 10;
  
  public static final int NOT_LOGGED_IN = 11;
  
  public static final int WAVE_RETRIEVAL_ERROR = 12;
  
  public static final int WAVELET_RETRIEVAL_ERROR = 13;
  
  public static final int INVALID_ID = 14;

  public static final int WEBSOCKET_ERROR = 15;
  
  private final int code;

  @JsIgnore
  public SException(int code) {
    this.code = code;
  }
  
  @JsIgnore
  public SException(int code, Throwable parent) {
    super(parent);
    this.code = code;
  }
  
  @JsIgnore
  public SException(int code, Throwable parent, String message) {
    super(message, parent);
    this.code = code;
  }
  
  @JsIgnore
  public SException(ResponseCode code) {
    this.code = code.getValue();
  }
  
  @JsIgnore
  public SException(ChannelException chException) {
    super(chException);
    this.code = chException.getResponseCode().getValue();   
  }
  
  /**
   * @return the error code.
   */
  @JsProperty
  public int getCode() {
    return code;
  }
  
  /**
   * @return the status code from a HTTP response.
   */
  @JsProperty
  public int getStatusCode() {
    if (super.getCause() != null && 
        super.getCause() instanceof HTTPOperationException) {
      HTTPOperationException opEx = (HTTPOperationException) super.getCause();
      return opEx.getStatusCode();
    }
    return -1;
  }
  
  /**
   * @return the status message from a HTTP response.
   */
  @JsProperty
  public String getStatusMessage() {
    if (super.getCause() != null && 
        super.getCause() instanceof HTTPOperationException) {
      HTTPOperationException opEx = (HTTPOperationException) super.getCause();
      return opEx.getStatusMessage();
    }
    return null;
  }
  
}
