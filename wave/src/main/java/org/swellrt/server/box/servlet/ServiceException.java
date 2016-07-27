package org.swellrt.server.box.servlet;

import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class ServiceException extends Exception {

	 int httpResponseCode;
	 String serviceResponseCode;
	
	public ServiceException(String message, int httpResponseCode, String serviceResponseCode, Throwable cause) {
		super(message, cause);
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
