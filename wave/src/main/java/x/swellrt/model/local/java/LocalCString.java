package x.swellrt.model.local.java;

import x.swellrt.model.CString;

public class LocalCString implements CString {

  
  protected String value;
  
  protected LocalCString(String value) {
    this.value = value;
  }
  
  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String s) {
    value = s;
  }

}
