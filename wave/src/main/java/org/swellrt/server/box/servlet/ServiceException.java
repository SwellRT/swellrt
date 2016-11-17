package org.swellrt.server.box.servlet;


@SuppressWarnings("serial")
public class ServiceException extends Exception {

	 private final int httpResponseCode;
	 private final String serviceResponseCode;
	
	public ServiceException(String message, int httpResponseCode, String serviceResponseCode, Throwable cause) {
		super(message, cause);
		this.httpResponseCode = httpResponseCode;
		this.serviceResponseCode = serviceResponseCode;
	}
	
	public ServiceException(String message, int httpResponseCode, String serviceResponseCode) {
		this(message, httpResponseCode, serviceResponseCode, null);
	}

	public int getHttpResponseCode() {
		return httpResponseCode;
	}

	public String getServiceResponseCode() {
		return serviceResponseCode;
	}
	

}
