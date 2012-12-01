package org.waveprotocol.box.common;

import java.util.ArrayList;

/**
 * Callback interface to sequential reception objects to list.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class ListReceiver<T> extends ArrayList<T> implements Receiver<T> {

  @Override
  public boolean put(T obj) {
    add(obj);
    return true;
  }
}
