package org.swellrt.beta.client.operation;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@SuppressWarnings("serial")
@JsType(namespace = "swellrt")
public class OperationException extends Exception {
	
	public static final String MISSING_PARAMETERS  = "missing_parameters";
	public static final String SESSION_NOT_STARTED  = "session_not_started";
	public static final String UNABLE_GET_OBJECT  = "unable_get_object";
	public static final String INVALID_OBJECT_ID  = "invalid_object_id";
	public static final String OPERATION_EXCEPTION = "operation_exception";
 		
	private final String errorCode;
	private final String errorMessage;
	private final Throwable exception;

	@JsIgnore
	public OperationException(String errorCode, String errorMessage) {
		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.exception = null;
	}

	@JsIgnore
	public OperationException(String errorCode, String errorMessage, Throwable e) {
		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.exception = e;
	}

	@JsProperty
	public String getErrorCode() {
		return errorCode;
	}

	@JsProperty
	public String getErrorMessage() {
		return errorMessage;
	}

	@JsProperty
	public Throwable getException() {
		return exception;
	}
	
	

}
