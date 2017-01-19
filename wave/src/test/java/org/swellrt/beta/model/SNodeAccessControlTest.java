package org.swellrt.beta.model;

import junit.framework.TestCase;

/*
 * @author pablojan@gmail.com
 *
 */
public class SNodeAccessControlTest extends TestCase {

  /**
   * Checks that serialization/deseralization works 
   * and return the access control object
   * @return
   */
  protected SNodeAccessControl deserializeWithAssert(String token) {
    SNodeAccessControl nac = SNodeAccessControl.deserialize(token);
   
    String tokenBis = nac.serialize();
    SNodeAccessControl nacBis = SNodeAccessControl.deserialize(tokenBis);
    
    assertTrue(nac.equals(nacBis));
    return nac;
  }
  
  public void testEmptyToken() {
    
    String t = "";
    SNodeAccessControl nac = deserializeWithAssert(t);
    
    
    assertTrue(nac.canRead("ann@acme.com"));
    assertTrue(nac.canRead("ann@acme.com"));
    
  }
  
  public void testReadersToken() {
    
    String t = "r[ann@acme.com,bob@acme.com]";
    SNodeAccessControl nac = deserializeWithAssert(t);
    
    
    assertTrue(nac.canRead("ann@acme.com"));
    assertTrue(nac.canRead("bob@acme.com"));
    assertFalse(nac.canRead("chris@acme.com"));

    assertTrue(nac.canWrite("xxx@acme.com"));
  }
  
  public void testOnlyWriteForbiddenToken() {
    
    String t = "!w";
    SNodeAccessControl nac = deserializeWithAssert(t);
    
    
    assertTrue(nac.canRead("ann@acme.com"));
    assertTrue(nac.canRead("bob@acme.com"));
    assertTrue(nac.canRead("chris@acme.com"));

    assertFalse(nac.canWrite("xxx@acme.com"));
  }
  
  public void testReadersWriteForbiddenToken() {
    
    String t = "r[ann@acme.com,bob@acme.com]!w";
    SNodeAccessControl nac = deserializeWithAssert(t);
    
    
    assertTrue(nac.canRead("ann@acme.com"));
    assertTrue(nac.canRead("bob@acme.com"));
    assertFalse(nac.canRead("chris@acme.com"));

    assertFalse(nac.canWrite("xxx@acme.com"));
  }
  
  public void testReadersWritersToken() {
    
    String t = "r[ann@acme.com,bob@acme.com]w[chris@acme.com,dany@acme.com]";
    SNodeAccessControl nac = deserializeWithAssert(t);
    

    assertTrue(nac.canRead("ann@acme.com"));
    assertTrue(nac.canRead("bob@acme.com"));
    assertFalse(nac.canRead("chris@acme.com"));
    assertFalse(nac.canRead("dany@acme.com"));
    assertFalse(nac.canRead("xxx@acme.com"));

    assertFalse(nac.canWrite("ann@acme.com"));
    assertFalse(nac.canWrite("bob@acme.com"));
    assertTrue(nac.canWrite("chris@acme.com"));
    assertTrue(nac.canWrite("dany@acme.com"));
    assertFalse(nac.canWrite("xxx@acme.com"));
  }
  
}
