package org.waveprotocol.box.server.authentication;

import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * A super session handling the standard HTTP session plus the Id of the
 * browser's window.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
public class HttpWindowSession implements HttpSession {

  private final HttpSession delegate;
  private final String windowId;


  public static HttpWindowSession of(HttpSession delegate, String windowId) {
    Preconditions.checkArgument(delegate != null, "Null http session");
    return new HttpWindowSession(delegate, windowId);

  }

  protected HttpWindowSession(HttpSession httpSession, String windowId) {
    this.delegate = httpSession;
    this.windowId = windowId;
  }

  public String getWindowId() {
    return windowId;
  }

  @Override
  public String toString() {
    return delegate.getId() + ":" + windowId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.getId().hashCode());
    result = prime * result + ((windowId == null) ? 0 : windowId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    HttpWindowSession other = (HttpWindowSession) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.getId().equals(other.getId())) return false;
    if (windowId == null) {
      if (other.windowId != null) return false;
    } else if (!windowId.equals(other.windowId)) return false;
    return true;
  }

  @Override
  public long getCreationTime() {
    return delegate.getCreationTime();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public long getLastAccessedTime() {
    return delegate.getLastAccessedTime();
  }

  @Override
  public ServletContext getServletContext() {
    return delegate.getServletContext();
  }

  @Override
  public void setMaxInactiveInterval(int interval) {
    delegate.setMaxInactiveInterval(interval);
  }

  @Override
  public int getMaxInactiveInterval() {
    return delegate.getMaxInactiveInterval();
  }

  @Deprecated
  @Override
  public HttpSessionContext getSessionContext() {
    return delegate.getSessionContext();
  }

  @Override
  public Object getAttribute(String name) {
    return delegate.getAttribute(name);
  }

  @Deprecated
  @Override
  public Object getValue(String name) {
    return delegate.getValue(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return delegate.getAttributeNames();
  }

  @Deprecated
  @Override
  public String[] getValueNames() {
    return delegate.getValueNames();
  }

  @Override
  public void setAttribute(String name, Object value) {
    delegate.setAttribute(name, value);
  }

  @Deprecated
  @Override
  public void putValue(String name, Object value) {
    delegate.putValue(name, value);
  }

  @Override
  public void removeAttribute(String name) {
    delegate.removeAttribute(name);
  }

  @Deprecated
  @Override
  public void removeValue(String name) {
    delegate.removeValue(name);
  }

  @Override
  public void invalidate() {
    delegate.invalidate();
  }

  @Override
  public boolean isNew() {
    return delegate.isNew();
  }

}
