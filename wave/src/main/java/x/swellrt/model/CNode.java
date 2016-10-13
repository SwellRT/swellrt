package x.swellrt.model;

import java.util.List;
import java.util.Map;

public interface CNode {
  
  public Map<String, CNode> asMap();
  
  public List<CNode> asList();
  
  public String asString();
  
  public int asInteger();
  
  public double asDouble();
  
  public boolean asBoolean();

  
}
