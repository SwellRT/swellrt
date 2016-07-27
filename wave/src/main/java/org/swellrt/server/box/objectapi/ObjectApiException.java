package org.swellrt.server.box.objectapi;

public class ObjectApiException extends Exception {

	private final String message;
	private final String code;
	
	
	
	public ObjectApiException(String message, String code) {
		super();
		this.message = message;
		this.code = code;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getCode() {
		return code;
	}
	
	
	
}
