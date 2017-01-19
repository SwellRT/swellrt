package org.swellrt.beta.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Utilities to manage access control tokens.
 * <p>
 * <br>
 * Access control token syntax is "{@code r[]w[tom@local.net,john@master.org]}"
 * where:
 * <li>if token is empty, access is granted for everyone, read and write</li>
 * <li>if "r" part is empty or not exist, everyone can read</li>
 * <li>if "r" part is not empty, read access is only granted for users in the list</li>
 * <li>if "w" part is empty or not exist, everyone can write</li>
 * <li>if "w" part is not empty, write access is only granted for users in the list</li>
 * <li>if there is a "!w" mark, the node is read only, and "w" list is ignored</li>
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SNodeAccessControl {
  
  public static final String ACCESS_TOKEN_READ_MARK = "r[";
  public static final String ACCESS_TOKEN_WRITE_MARK = "w[";
  public static final String ACCESS_TOKEN_READONLY_MARK = "!w";
  
  public static boolean isToken(String s) {
    return s != null && 
        (s.startsWith(ACCESS_TOKEN_READ_MARK) ||
        s.startsWith(ACCESS_TOKEN_WRITE_MARK) ||
        s.startsWith(ACCESS_TOKEN_READONLY_MARK));
  }
  
  public static class Builder {
    
    List<String> readers = new ArrayList<String>();
    List<String> writers = new ArrayList<String>();
    boolean isReadOnly = false;
    
    public Builder read(String[] participants) {
      if (participants != null && participants.length > 0)
        for (int i = 0; i < participants.length; i++)
          readers.add(participants[i]);
      
      return this;
    }
    
    public Builder write(String[] participants) {
      if (participants != null && participants.length > 0)
        for (int i = 0; i < participants.length; i++)
          writers.add(participants[i]);
      
      return this;
    }
    
    public Builder read(String participantId) {
      if (participantId != null)
        readers.add(participantId);
      return this;
    }
    
    public Builder write(String participantId) {
      if (participantId != null)
        writers.add(participantId);
      return this;
    }
    
    public Builder setReadOnly(Boolean isReadOnly) {
      if (isReadOnly != null)
        this.isReadOnly = isReadOnly;
      return this;
    }
    
    public SNodeAccessControl build() {
      return new SNodeAccessControl(readers, writers, isReadOnly);
    }
  }
  
  public static SNodeAccessControl deserialize(String s) {
    if (s == null) return null;
    
    boolean isReadOnly = s.indexOf("!w") >= 0;
    
    int rbegin = s.indexOf("r[");
    String[] readers;
    if (rbegin >= 0) {
      int rend = s.indexOf("]", rbegin);
      String rstr = s.substring(rbegin+2, rend);
      readers = rstr.split(",");
    } else {
      readers = new String[0];
    }
  
    int wbegin = s.indexOf("w[");
    String[] writers;
    if (wbegin >= 0) {
      int wend = s.indexOf("]", wbegin);
      String wstr = s.substring(wbegin+2, wend);
      writers = wstr.split(",");
    } else {
      writers = new String[0];
    }
    
    return new SNodeAccessControl(Arrays.asList(readers), Arrays.asList(writers), isReadOnly);
  }
  
  
  private final Set<String> readers = new HashSet<String>();
  private final Set<String> writers = new HashSet<String>();
  private boolean isReadOnly = false;
  
  public SNodeAccessControl(Collection<String> readers, Collection<String> writers, boolean isReadOnly) {
    this.readers.addAll(readers);
    this.writers.addAll(writers);
    this.isReadOnly = isReadOnly;
  }
  
  public SNodeAccessControl(boolean isReadOnly) {
    this.isReadOnly = isReadOnly;
  }
  
  public SNodeAccessControl() {
  }
    
  public boolean canWrite(ParticipantId participantId) {
    return canWrite(participantId.getAddress());
  }
  
  public boolean canWrite(String participantId) {
    return !isReadOnly && (writers.isEmpty() || writers.contains(participantId)) ;
  }
  
  public boolean canRead(ParticipantId participantId) {
    return canRead(participantId.getAddress());
  }

  public boolean canRead(String participantId) {
    return readers.isEmpty() || readers.contains(participantId);
  }
  
  public String serialize() {
    String w = "";
    if (!writers.isEmpty()) {
      w+="w[";
      for (String p: writers) {
        if (w.length() > 2)
          w += ",";
        w+=p;
      }
      w+="]";
    }
    
    if (isReadOnly)
      if (w.length() > 0)
        w = "!"+w;
      else
        w = "!w";
    
    String r = "";
    if (!readers.isEmpty()) {
      r+="r[";
      for (String p: readers) {
        if (r.length() > 2)
          r += ",";
        r+=p;
      }
      r+="]";
    }
    
    return r+w;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isReadOnly ? 1231 : 1237);
    result = prime * result + ((readers == null) ? 0 : readers.hashCode());
    result = prime * result + ((writers == null) ? 0 : writers.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SNodeAccessControl other = (SNodeAccessControl) obj;
    if (isReadOnly != other.isReadOnly) {
      return false;
    }
    
    if (readers == null) {
      if (other.readers != null) {
        return false;
      }
    } else if (readers.size() != other.readers.size()) {
      return false;
    } else {
      for (String s: readers) {
        if (!other.readers.contains(s))
          return false;
      }
    }
    
    
    if (writers == null) {
      if (other.writers != null) {
        return false;
      }
    } else if (writers.size() != other.writers.size()) {
      return false;
    } else {
      for (String s: writers) {
        if (!other.writers.contains(s))
          return false;
      }
    }
    
    return true;
  }
  
  
}
