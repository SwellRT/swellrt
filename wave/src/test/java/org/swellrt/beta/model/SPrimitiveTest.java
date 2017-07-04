package org.swellrt.beta.model;

import junit.framework.TestCase;

public class SPrimitiveTest extends TestCase {
  
  
  public void testSerialization() {
    
    SNodeAccessControl.Builder nacBuilder = new SNodeAccessControl.Builder();
    nacBuilder.read("ann@acme.com");
    nacBuilder.read("bob@acme.com");
    nacBuilder.write("ann@acme.com");
    nacBuilder.write("chris@acme.com");
    nacBuilder.setReadOnly(true);
    
    SPrimitive pString = new SPrimitive("Hello World", nacBuilder.build());
    String pStringAsString = pString.serialize();
    
    SPrimitive pStringBis = SPrimitive.deserialize(pStringAsString);
    
    assertEquals(pString.value(), pStringBis.value());
    assertEquals(pStringBis.getNodeAccessControl(), pString.getNodeAccessControl());
  }

}
