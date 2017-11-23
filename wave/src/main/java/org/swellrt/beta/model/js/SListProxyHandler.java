package org.swellrt.beta.model.js;


import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SNode;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * A Javascript proxy for SList.
 * <p><br>
 * Limitations:
 * <p>
 * <li>push() only allow one parameter</li>
 * <li>only push(), pop(), shift() and unshift() methods are implemented</li>
 *
 * <p><br>
 * TODO implement unit test
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swell")
public class SListProxyHandler extends ProxyHandler {


  @JsFunction
  public interface Func {
    Object exec(Object argument);
  }

  public interface ArrayOp {
   @SuppressWarnings("rawtypes")
  Object exec(SList target, int index, Object data) throws SException;
  }

  @JsIgnore
  public SListProxyHandler() {
  }

  /**
   * Returns the proxy or primitive value
   *
   * @param node
   * @return
   */
  protected static Object getProxy(Object node) {
    if (node instanceof HasJsProxy) {
      Proxy proxy = ((HasJsProxy) node).getJsProxy();
      if (proxy == null) {
        proxy = new Proxy((SNode) node, new SListProxyHandler());
        ((HasJsProxy) node).setJsProxy(proxy);
      }
      return proxy;
    } else {
      return node;
    }
  }

  /**
   * Executes an operation over the list if the property (index) is valid.
   * @param target
   * @param property
   * @param value
   * @param op
   * @return
   * @throws SException
   */
  @SuppressWarnings("rawtypes")
  private static Object executeSafeArrayOp(SList target, String property, Object value, ArrayOp op) throws SException {

    boolean isInt = true;
    int index = -1;
    try {
      index = Integer.valueOf(property);
    } catch (Exception e) {
      isInt = false;
    }

    if (isInt) {
      if (index >= 0 && index < target.size()) {
        return op.exec(target, index, value);
      }
    }

    return null;
  }


  @SuppressWarnings("rawtypes")
  public Object get(SList target, String property, ProxyHandler receiver) throws SException {

    if (property == null)
      return Global.getUndefined();


    Object result = executeSafeArrayOp(target, property, null, new ArrayOp() {
      @Override
      public Object exec(SList target, int index, Object data) throws SException {
        return getProxy(target.pick(index));
      }
    });

    if (result != null) return result;

    String method = property;

    if ("length".equals(method)) {
      return target.size();
    }

    if ("push".equals(method)) {

      return new Func() {

        @Override
        public Object exec(Object argument) {
          try {
            target.add(argument);
          } catch (SException e) {
            return null;
          }
          return target.size();
        }

      };

    }

    if ("pop".equals(method)) {

      return new Func() {

        @Override
        public Object exec(Object argument) {
          Object o = null;
          try {
            int last = target.size()-1;
            o = target.pick(last);
            target.remove(last);
          } catch (SException e) {
          }
          return o;
        }

      };

    }

    if ("unshift".equals(method)) {

    }

    if ("shift".equals(method)) {

    }

    if ("join".equals(method)) {
      // not implemented
    }

    if ("reverse".equals(method)) {
      // not implemented
    }

    if ("sort".equals(method)) {
      // not implemented
    }

    if ("concat".equals(method)) {
      // not implemented
    }

    if ("slice".equals(method)) {
      // not implemented
    }

    if ("splice".equals(method)) {
      // not implemented
    }

    if ("toString".equals(method) ||
        "toLocaleString".equals(method)) {
      // not implemented
    }

    return Global.getUndefined();
  }




  @SuppressWarnings("rawtypes")
  public Object set(SList target, String property, Object value, ProxyHandler receiver) throws SException {

    if (property == null)
      return false;

    // this method doesn't allow to insert at the end of the list!!!
    Object result = executeSafeArrayOp(target, property, value, new ArrayOp() {

      @Override
      public Object exec(SList target, int index, Object data) throws SException {

        if (value != null) {
          // add
          target.addAt(value, index);
          return getProxy(target.pick(index));
        } else {
          // remove
          Object removedValue = target.pick(index);
          target.remove(index);
          return getProxy(removedValue);
        }

      }

    });

    if (result != null)
      return result;

    return Reflect.set(target, property, value);

  }

  @SuppressWarnings("rawtypes")
  public boolean has(SList target, String property) throws SException {

    if (property == null)
      return false;

    Object result = null;

    try {
      result = executeSafeArrayOp(target, property, null, new ArrayOp() {
        @Override
        public Object exec(SList target, int index, Object data) throws SException {
          return true;
        }
      });
    } catch (Exception e) {
    }

    if (result != null && "length".equals(property))
      return true;

    return Reflect.has(target, property);

  }

  @SuppressWarnings("rawtypes")
  public Object ownKeys(SList target) throws SException {

    JsArrayString keys = JavaScriptObject.createArray().<JsArrayString>cast();

    for (int i = 0; i < target.size(); i++)
      keys.push(""+i);

    keys.push("length");

    return keys;
  }

  @SuppressWarnings("rawtypes")
  public Object getOwnPropertyDescriptor(SList target, String property) throws SException {

    if (property == null)
      return Global.getUndefined();

    Object result = executeSafeArrayOp(target, property, null, new ArrayOp() {

      @Override
      public Object exec(SList target, int index, Object data) throws SException {
       JsoView descriptor = JsoView.as(JavaScriptObject.createObject());
        descriptor.setObject("value", target.pick(index));
       descriptor.setBoolean("writable", true);
       descriptor.setBoolean("enumerable", true);
       descriptor.setBoolean("configurable", true);
       return descriptor.cast();
      }
    });

    if (result != null)
      return result;

    if ("length".equals(property)) {
      JsoView descriptor = JsoView.as(JavaScriptObject.createObject());
      descriptor.setObject("value", target.size());
      descriptor.setBoolean("writable", true);
      descriptor.setBoolean("enumerable", false);
      descriptor.setBoolean("configurable", false);
      return descriptor.cast();
    }

    return Reflect.getOwnPropertyDescriptor(target, property);
  }

  @SuppressWarnings("rawtypes")
  public boolean defineProperty(SList target, String property, Object descriptor) throws SException {
    return Reflect.defineProperty(target, property, descriptor);
  }


  @SuppressWarnings("rawtypes")
  public boolean deleteProperty(SList target, String property) throws SException {

    Object result = executeSafeArrayOp(target, property, null, new ArrayOp() {

      @Override
      public Object exec(SList target, int index, Object data) throws SException {
        target.remove(index);
        return true;
      }

    });


    return result != null;

  }
}
