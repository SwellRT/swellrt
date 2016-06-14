package org.swellrt.api;

public interface OnLoadCallback<T> {


  public void onLoad(T param);

  public void onError(String reason);

}
