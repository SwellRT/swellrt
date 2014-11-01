package org.waveprotocol.mod.model.p2pvalue.docindex;


import org.waveprotocol.mod.model.p2pvalue.docindex.DocIndex.Action;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A ElementList of Documents. It is supported by an Index Doc and the
 * Wavelet.
 *
 * The Index Doc is shared. It allows to track deleted Docs/Elements of any
 * type.
 *
 * An ElementList refers to a specific kind of Docs that shares a common prefix.
 *
 * @author pablojan@gmail.com
 *
 * @param <T> The DocBasedXXX type
 * @param <I> The initialization data type
 */
public class IndexBasedElementList<T extends DocIndexed, I> implements ObservableElementList<T, I>,
    DocIndex.Listener {


  protected String docPrefix;
  protected DocIndex docIndex;
  protected DocIndexedFactory<T> factory;
  protected List<T> elements;
  protected Set<String> elementsId;

  protected List<ObservableElementList.Listener<T>> listeners;


  public static <T extends DocIndexed, I> IndexBasedElementList<T, I> create(String docPrefix,
      DocIndex docIndex, DocIndexedFactory<T> factory) {
    return new IndexBasedElementList<T, I>(docPrefix, docIndex, factory);
  }


  private IndexBasedElementList(String docPrefix, DocIndex docIndex,
      DocIndexedFactory<T> factory) {
    this.docPrefix = docPrefix;
    this.docIndex = docIndex;
    this.factory = factory;
    this.elements = null;
    this.elementsId = null;

    this.listeners = new ArrayList<ObservableElementList.Listener<T>>();
    this.docIndex.addListener(this);
  }

  @Override
  public Iterable<T> getValues() {

    if (elements == null) {

      elements = new ArrayList<T>();
      elementsId = new HashSet<String>();

      final List<T> values = elements;
      final Set<String> ids = elementsId;

      docIndex.traverseDocuments(new Action() {

        @Override
        public void execute(String docId) {
          ObservableDocument doc = docIndex.getDocument(docId);
          values.add(factory.adapt(doc, docId));
          ids.add(docId);
        }

      });
    }


    return elements;
  }

  protected void remove(int index) {

    T docElement = elements.get(index);
    elements.remove(index);
    elementsId.remove(docElement.getDocumentId());

    for (Listener<T> l : listeners) {
      l.onValueRemoved(docElement);
    }

  }


  @Override
  public boolean remove(T element) {

    String docId = element.getDocumentId();
    if (!elementsId.contains(docId)) return false;

    T docElement = null;
    int i = 0;
    for (T e : elements) {
      if (e.getDocumentId().equals(docId)) {
        docElement = e;
        break;
      }
      i++;
    }

    Preconditions.checkNotNull(docElement, "Removing Indexed Document but not found");

    remove(i);

    return true;

  }

  @Override
  public T add(I initialState) {
    return add(elements.size(), initialState);
  }


  @Override
  public T add(int index, I initialState) {

    if (index > elements.size() || index < 0) return null;

    Pair<String, ObservableDocument> docPair = docIndex.createDocument(docPrefix);

    T docElement = factory.adapt(docPair.second, docPair.first);

    elements.add(index, docElement);
    elementsId.add(docPair.first);

    for (Listener<T> l : listeners) {
      l.onValueAdded(docElement);
    }

    return docElement;
  }

  @Override
  public int indexOf(T element) {

    int i = 0;
    for (T e : elements) {
      if (e.equals(element)) break;
      i++;
    }

    return i < elements.size() ? i : -1;
  }

  @Override
  public T get(int index) {
    return elements.get(index);
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public void clear() {
    for (int i = 0; i < elements.size(); i++) {
      remove(i);
    }
  }



  @Override
  public void addListener(org.waveprotocol.wave.model.adt.ObservableElementList.Listener<T> listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(
      org.waveprotocol.wave.model.adt.ObservableElementList.Listener<T> listener) {
    listeners.remove(listener);
  }

  //
  // DocIndex Listener
  //

  @Override
  public void onDocumentAdded(String docId) {
    if (IdUtil.getInitialToken(docId).equals(docPrefix)) {
      if (!elementsId.contains(docId)) {
        T docElement = factory.adapt(docIndex.getDocument(docId), docId);
        elements.add(docElement);
        elementsId.add(docId);

        for (Listener<T> l : listeners) {
          l.onValueAdded(docElement);
        }

      }
    }
  }


  @Override
  public void onDocumentRemoved(String docId) {
    if (IdUtil.getInitialToken(docId).equals(docPrefix)) {

      if (elementsId.contains(docId)) {

        T docElement = null;
        int i = 0;
        for (T e: elements) {
          if (e.getDocumentId().equals(docId)) {
            docElement = e;
            break;
          }
          i++;
        }

        Preconditions.checkNotNull(docElement, "Removing Indexed Document but not found");

        elements.remove(i);
        elementsId.remove(docId);

        for (Listener<T> l : listeners) {
          l.onValueRemoved(docElement);
        }

      }
    }
  }


}
