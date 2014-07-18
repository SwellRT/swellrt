package org.waveprotocol.box.webclient.client.extended.ui;

public class StringItem implements ListItem {

  private String value;

  public StringItem(String value) {
    this.value = value;
  }

  @Override
  public String asString() {
    return value;
  }

}
