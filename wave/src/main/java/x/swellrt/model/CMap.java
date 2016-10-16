package x.swellrt.model;

import java.util.Map;

public interface CMap extends CNode, Map<String, CNode> {
    
  public CNode put(String key, String value);
  
  public CNode put(String key, int value);
  
  public CNode put(String key, double value);
  
  public CNode put(String key, boolean value);
  
}
