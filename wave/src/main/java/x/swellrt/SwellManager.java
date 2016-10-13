package x.swellrt;

import x.swellrt.model.CObject;

public interface SwellManager {

  public interface Callback<T> {
            
  }
  
  public interface OptionsOpen {
    
  }
  
  public interface OptionsClose {
    
  }
  
  public interface OptionsQuery {
    
    
  }
  
  
  public void open(OptionsOpen options, Callback<CObject> onComplete);
  
  
  public void close(OptionsClose options, Callback<Void> onComplete);
  
  
  public void query(OptionsQuery options, Callback<Void> onComplete);
  
}
