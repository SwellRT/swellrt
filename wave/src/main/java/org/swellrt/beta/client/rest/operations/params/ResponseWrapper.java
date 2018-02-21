package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.rest.ServiceOperation;

public class ResponseWrapper implements ServiceOperation.Response {

  private String data;

  public void setResponse(String data) {
    this.data = data;
  }

  public String getResponseString() {
    return data;
  }

}
