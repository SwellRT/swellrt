package org.waveprotocol.box.server.swell.rest;

import org.waveprotocol.wave.federation.ProtocolDocumentOperation;
import org.waveprotocol.wave.federation.ProtocolDocumentOperation.Component.AnnotationBoundary;
import org.waveprotocol.wave.federation.ProtocolDocumentOperation.Component.ElementStart;
import org.waveprotocol.wave.federation.ProtocolDocumentOperation.Component.KeyValuePair;
import org.waveprotocol.wave.federation.ProtocolDocumentOperation.Component.KeyValueUpdate;
import org.waveprotocol.wave.federation.ProtocolDocumentOperation.Component.ReplaceAttributes;
import org.waveprotocol.wave.federation.ProtocolDocumentOperation.Component.UpdateAttributes;
import org.waveprotocol.wave.federation.gson.ProtocolDocumentOperationGsonImpl;
import org.waveprotocol.wave.federation.gson.ProtocolDocumentOperationGsonImpl.ComponentGsonImpl;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;

/**
 *
 */
public class WaveProtocolSerializer {

  private WaveProtocolSerializer() {

  }

  /**
   * Serializes a {@link DocOp} as a {@link ProtocolDocumentOperation}.
   *
   * Adapted version for {@link ComponentGsonImpl} components. Components are
   * populated before being added to
   * {@link ProtocolDocumentOperationGsonImpl#addComponent} because this method
   * adds a copy of the argument value (a hard pass-by-value semantic).
   *
   * @param inputOp
   *          document operation to serialize
   * @return serialized protocol buffer document operation
   */
  public static ProtocolDocumentOperationGsonImpl serialize(DocOp inputOp) {
    final ProtocolDocumentOperationGsonImpl output = new ProtocolDocumentOperationGsonImpl();
    inputOp.apply(new DocOpCursor() {

      private void addComponent(ComponentGsonImpl component) {
        output.addComponent(component);
      }

      private KeyValuePair keyValuePair(String key, String value) {
        KeyValuePair pair = new ComponentGsonImpl.KeyValuePairGsonImpl();
        pair.setKey(key);
        pair.setValue(value);
        return pair;
      }

      private KeyValueUpdate keyValueUpdate(String key, String oldValue, String newValue) {
        KeyValueUpdate kvu = new ComponentGsonImpl.KeyValueUpdateGsonImpl();
        kvu.setKey(key);
        if (oldValue != null) {
          kvu.setOldValue(oldValue);
        }
        if (newValue != null) {
          kvu.setNewValue(newValue);
        }
        return kvu;
      }

      @Override
      public void retain(int itemCount) {
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setRetainItemCount(itemCount);
        addComponent(component);
      }

      @Override
      public void characters(String characters) {
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setCharacters(characters);
        addComponent(component);
      }

      @Override
      public void deleteCharacters(String characters) {
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setDeleteCharacters(characters);
        addComponent(component);
      }

      @Override
      public void elementStart(String type, Attributes attributes) {
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setElementStart(makeElementStart(type, attributes));
        addComponent(component);
      }

      @Override
      public void deleteElementStart(String type, Attributes attributes) {
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setDeleteElementStart(makeElementStart(type, attributes));
        addComponent(component);
      }

      private ElementStart makeElementStart(String type, Attributes attributes) {
        ElementStart e = new ComponentGsonImpl.ElementStartGsonImpl();
        e.setType(type);
        for (String name : attributes.keySet()) {
          e.addAttribute(keyValuePair(name, attributes.get(name)));
        }
        return e;
      }

      @Override
      public void elementEnd() {
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setElementEnd(true);
        addComponent(component);
      }

      @Override
      public void deleteElementEnd() {
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setDeleteElementEnd(true);
        addComponent(component);
      }

      @Override
      public void replaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        ReplaceAttributes r = new ComponentGsonImpl.ReplaceAttributesGsonImpl();
        if (oldAttributes.isEmpty() && newAttributes.isEmpty()) {
          r.setEmpty(true);
        } else {
          for (String name : oldAttributes.keySet()) {
            r.addOldAttribute(keyValuePair(name, oldAttributes.get(name)));
          }

          for (String name : newAttributes.keySet()) {
            r.addNewAttribute(keyValuePair(name, newAttributes.get(name)));
          }
        }

        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setReplaceAttributes(r);
        addComponent(component);
      }

      @Override
      public void updateAttributes(AttributesUpdate attributes) {
        UpdateAttributes u = new ComponentGsonImpl.UpdateAttributesGsonImpl();
        if (attributes.changeSize() == 0) {
          u.setEmpty(true);
        } else {
          for (int i = 0; i < attributes.changeSize(); i++) {
            u.addAttributeUpdate(keyValueUpdate(attributes.getChangeKey(i),
                attributes.getOldValue(i), attributes.getNewValue(i)));
          }
        }
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setUpdateAttributes(u);
        addComponent(component);
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
        AnnotationBoundary a = new ComponentGsonImpl.AnnotationBoundaryGsonImpl();
        if (map.endSize() == 0 && map.changeSize() == 0) {
          a.setEmpty(true);
        } else {
          for (int i = 0; i < map.endSize(); i++) {
            a.addEnd(map.getEndKey(i));
          }
          for (int i = 0; i < map.changeSize(); i++) {
            a.addChange(
                keyValueUpdate(map.getChangeKey(i), map.getOldValue(i), map.getNewValue(i)));
          }
        }
        ComponentGsonImpl component = new ComponentGsonImpl();
        component.setAnnotationBoundary(a);
        addComponent(component);
      }
    });
    return output;
  }

}
