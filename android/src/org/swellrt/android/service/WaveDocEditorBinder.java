package org.swellrt.android.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.waveprotocol.wave.concurrencycontrol.wave.CcDataDocumentImpl;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Doc.T;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.ContentDeleted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.ContentInserted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.TextDeleted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.TextInserted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.Type;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Preconditions;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

/**
 * Instances of this class bind an EditText component to a Wave's document, so
 * remote changes from a doc are adapted and applied to the editor's view and
 * changes from the editor are adapted and sent to the doc.
 *
 * A doc's tag index is kept to convert view's cursor positions from/to doc's
 * positions.
 *
 * TODO Avoid event eco: when a operation is send to the doc, it is send-back as
 * an event which must be ignored.
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 */
public class WaveDocEditorBinder implements TextWatcher, DocumentHandler<N, E, T> {

  public static final String TAG = "WaveDocEditorBinder";

  public class Tag {

    String tag;
    /** Cursor position of the tag in the Wave's doc. */
    int docPos;
    /** Cursor position of the tag in the Android's view. */
    int viewPos;
    /**
     * Length of this tag element in Wave's doc. Always associated with the
     * closing tag.
     */
    int span = 1;
    /**
     * Length of this tag element as visible element in the view. Always
     * associated with the closing tag.
     */
    int visibleSpan = 0;

    public Tag(String tag, int docPos, int viewPos, int span, int visibleSpan) {
      this.tag = tag;
      this.docPos = docPos;
      this.viewPos = viewPos;
      this.span = span;
      this.visibleSpan = visibleSpan;
    }

    @Override
    public String toString() {
      return tag + " w:" + docPos + " a:" + viewPos;
    }
  }


  private List<Tag> tagIndex = new ArrayList<Tag>();
  private final CcDataDocumentImpl doc;
  private final EditText editor;

  StringBuilder viewTextBuilder;

  /**
   * A cursor to trasverse a Wave's doc XML and build a tag index and a
   * visualizable format of the text for Android's TextView
   */
  private DocInitializationCursor tagIndexAndTextBuilder = new DocInitializationCursor() {

    /** Wave's doc pointer, position of the next element */
    private int docIndex = 0;

    /** Android's view pointer, position of the next element */
    private int viewIndex = -1;

    Deque<String> tagQueue = new ArrayDeque<String>();

    private int span = 0;

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
    }

    @Override
    public void characters(String chars) {

      if (tagQueue.size() == 1) { // we are in body
        docIndex += chars.length();
        viewIndex += chars.length();
      } else if (tagQueue.size() > 1) { // we are inside a tag
        span += chars.length();
      }


      viewTextBuilder.append(chars);
    }

    @Override
    public void elementStart(String type, Attributes attrs) {

      tagQueue.push(type);

      if (tagQueue.size() == 1) { // we have added body
        Tag tag = new Tag(type, docIndex, viewIndex, 1, 0);
        tagIndex.add(tag);
        docIndex++;
      } else if (tagQueue.size() > 1) { // we are inside a tag
        span++;
      }
    }

    @Override
    public void elementEnd() {

      String type = tagQueue.pop();

      if (tagQueue.size() == 0) { // we have removed body

        Tag tag = new Tag(type + "/", docIndex++, viewIndex, 1, 0);
        tagIndex.add(tag);

      } else if (tagQueue.size() == 1) { // closing a first level tag

        int visibleSpan = 0;
        if (type.equals("line")) {
          visibleSpan = 1;
          viewTextBuilder.append('\n');
        }

        if (visibleSpan > 0)
          viewIndex++;

        span++; // count the element end position

        Tag tag = new Tag(type, docIndex, viewIndex, span, visibleSpan);
        tagIndex.add(tag);

        docIndex += span;
        viewIndex += visibleSpan;
        span = 0;

      } else if (tagQueue.size() > 1) { // we are inside a tag
        span++;
      }

    }

  };

  public static WaveDocEditorBinder bind(EditText editor, CcDataDocumentImpl doc) {
    WaveDocEditorBinder binder = new WaveDocEditorBinder(editor, doc);
    binder.init();
    return binder;
  }

  private WaveDocEditorBinder(EditText editor, CcDataDocumentImpl doc) {
    this.editor = editor;
    this.doc = doc;
  }

  private void init() {

    // Init data structures
    viewTextBuilder = new StringBuilder();
    tagIndex.clear();

    // Traverse the document, build the view's text and the tag index
    doc.asOperation().apply(tagIndexAndTextBuilder);

    // Some loggind
    Log.d(TAG, "Doc: " + doc.toXmlString());
    Log.d(TAG, "Tag index: " + tagIndex.toString());

    // Bind editor and doc with this object
    editor.setText(viewTextBuilder);
    editor.addTextChangedListener(this);
    doc.addListener(this);
  }



  /**
   * Transforms a cursor position in an TextView to a doc position
   *
   * if viewPos is a text character returns the equivalent position in the doc.
   * if viewPos is a character assigned to an element (e.g. line, image) returns
   * the position of the start tag in the doc.
   *
   * @param viewPos
   *          The cursor position in the Android's view of the doc
   * @return The equivalent cursor position in the original Wave's doc
   */
  private int getDocPos(int viewPos) {

    int offset = 0;
    for (Tag tag : tagIndex) {
      if (tag.viewPos < viewPos) {
        offset += (tag.span - tag.visibleSpan);
      } else {
        break;
      }
    }

    return viewPos + offset;
  }

  /**
   * Transforms a doc position into a cursor position in a TextView
   *
   * @param wavePos
   * @return
   */
  private int getViewPos(int docPos) {

    int offset = 0;
    for (Tag tag : tagIndex) {
      if (tag.docPos < docPos) {
        offset += (tag.span - tag.visibleSpan);
      } else if (tag.docPos <= docPos && docPos < (tag.docPos + tag.span)) {
        return tag.viewPos;
      } else {
        break;
      }
    }

    return docPos > offset ? docPos - offset : 0;
  }



  //
  // Update tag index from View's events
  //

  private void deleteDocRange(int startDocPos, int endDocPos) {

  }

  //
  // Update tag index from Doc's events
  //

  /**
   * Update the tag index. Text doesn't contain tags.
   *
   * @param docPos
   * @param text
   */
  private void insertDocText(int docPos, int textLength) {
    for (Tag tag : tagIndex) {
      if (tag.docPos >= docPos) {
        tag.docPos += textLength;
        tag.viewPos += textLength;
      }
    }
  }

  /**
   * Update the tag index. Text doesn't countain tags.
   *
   * @param docPos
   *          the start position of deleted text
   * @param textLenght
   *          the length of deleted text
   */
  private void deleteDocText(int docPos, int textLength) {
    for (Tag tag : tagIndex) {
      if (tag.docPos >= docPos + textLength) {
        tag.docPos -= textLength;
        tag.viewPos -= textLength;
      }
    }
  }


  /**
   * Insert a tag in the tag index offsetting elements after the new tag.
   *
   */
  private void insertDocTag(String tagStr, int docPos, int elementLenght) {

    int viewPos = getViewPos(docPos);
    int visibleSpan = 0;
    int span = elementLenght;

    // TODO: We only expect lines, take care of images... later
    if (tagStr.equals("line")) {
      visibleSpan = 1;
      span = 2;
    }

    Tag newTag = new Tag(tagStr, docPos, viewPos, span, visibleSpan);

    // Insert tag keeping index sorting
    int c = 0;
    for (Tag tag : tagIndex) {
      if (tag.docPos >= docPos) { // we have found the place
        tagIndex.add(c++, newTag);
        break;
      }
      c++;
    }

    // Update the rest of the index
    for (int i = c; i < tagIndex.size(); i++) {
      Tag tag = tagIndex.get(i);
      tag.docPos += newTag.span;
      tag.viewPos += newTag.visibleSpan;
    }

  }


  /**
   * Delete a tag in the tag index offsetting elements after the new tag.
   *
   * @param docPos
   */
  private void deleteDocTag(int docPos) {

    // Look up the tag, it must be in the index
    // Update upper tags in the index in the same loop.
    int c = 0;
    int toDeleteIndex = -1;
    Tag toDeleteTag = null;

    for (Tag tag : tagIndex) {
      if (tag.docPos == docPos) {
        toDeleteIndex = c;
        toDeleteTag = tag;
      } else if (tag.docPos > docPos) {
        tag.docPos -= toDeleteTag.span;
        tag.viewPos -= toDeleteTag.visibleSpan;
      }
      c++;
    }

    Preconditions.checkNotNull(toDeleteTag,
        "A tag to be delete was not found in the tag index");

    tagIndex.remove(toDeleteIndex);

  }


  //
  // TextWatcher: listening view changes
  //

  @Override
  public void afterTextChanged(Editable s) {
    // Nothing to do
  }


  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    // delete chars
    if (count > 0) {

      // Transform text editor cursor position to Wave's doc positions
      int startDocPos = getDocPos(start);
      int endDocPos = getDocPos(start + count);

      Log.d(TAG, "Deleting chars: view(" + start + "," + (start + count) + ") doc("
          + startDocPos + "," + endDocPos + ")");

      doc.removeListener(this);

      // Update doc
      doc.deleteRange(startDocPos, endDocPos);


      // Update tag index
      deleteDocRange(startDocPos, endDocPos);

      doc.addListener(this);
    }
  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {

    // insert chars

    if (count > 0) {

      // Slice text in chunks separated by breaklines (or other tag-related
      // components in the future)
      // TODO add support to more tags with view's correspondance, by now only
      // breaklines
      String chars = s.subSequence(start, start + count).toString();

      int partStartIndex = 0;
      int partEndIndex = -1;
      while (partStartIndex < chars.length()) {

        boolean breaklineFound = false;

        partEndIndex = chars.indexOf('\n', partStartIndex); // ab\n
        if (partEndIndex == -1)
          partEndIndex = chars.length();
        else
          breaklineFound = true;

        int partLength = partEndIndex - partStartIndex;
        int partDocPos = getDocPos(start + partStartIndex);

        // Add text

        Log.d(TAG, "Inserting " + partLength + " chars at view(" + (start + partStartIndex)
            + ") doc(" + partDocPos + ")");

        doc.removeListener(this);

        // Update doc
        doc.insertText(partDocPos, chars.substring(partStartIndex, partEndIndex));


        // Update tag index
        insertDocText(partDocPos, partLength);

        // Add breakline
        if (breaklineFound) {
          Point<N> point = doc.locate(partDocPos + partLength);
          doc.createElement(point, "line", Collections.<String, String>emptyMap());
        }

        doc.addListener(this);

        // follow with next substring
        partStartIndex = partEndIndex + 1; // Breakline is only 1 char lenght
      }

    }
  }


  //
  // DocumenHandler: listening remote changes
  //

  @Override
  public void onDocumentEvents(
      org.waveprotocol.wave.model.document.indexed.DocumentHandler.EventBundle<N, E, T> eventBundle) {

    // Disable listening changes in the editor to avoid eco
    editor.removeTextChangedListener(this);
    editor.beginBatchEdit();

    for (DocumentEvent<N, E, T> event : eventBundle.getEventComponents()) {

      if (event.getType() == Type.TEXT_DELETED) {
        TextDeleted<N, E, T> deleteTextEvent = (TextDeleted<N, E, T>) event;

        int startViewPos = getViewPos(deleteTextEvent.getLocation());
        int length = deleteTextEvent.getDeletedText().length();

        // Update view
        editor.getText().delete(startViewPos, startViewPos + length);

        // Update tag index
        deleteDocText(deleteTextEvent.getLocation(), length);


      } else if (event.getType() == Type.TEXT_INSERTED) {
        TextInserted<N, E, T> insertTextEvent = (TextInserted<N ,E ,T>) event;

        int startViewPos = getViewPos(insertTextEvent.getLocation());

        // Update View
        editor.getText().insert(startViewPos, insertTextEvent.getInsertedText());

        // Update tag index
        insertDocText(insertTextEvent.getLocation(), insertTextEvent.getInsertedText().length());

      } else if (event.getType() == Type.CONTENT_INSERTED) {
        ContentInserted<N, E, T> contentInsertedEvent = (ContentInserted<N, E, T>) event;

        int docPos = doc.getLocation(contentInsertedEvent.getSubtreeElement());
        int elementLenght = DocHelper.getItemSize(doc, contentInsertedEvent.getSubtreeElement());
        String tagStr = doc.getTagName(contentInsertedEvent.getSubtreeElement());

        // Update View
        // TODO support more visualizable tags
        if (tagStr.equals("line")) {
          int viewPos = getViewPos(docPos);
          editor.getText().insert(viewPos, "\n");
        }

        // Update tag index
        insertDocTag(tagStr, docPos, elementLenght);


      } else if (event.getType() == Type.CONTENT_DELETED) {
        ContentDeleted<N, E, T> contentDeletedEvent = (ContentDeleted<N, E, T>) event;

        // Update View
        // TODO support mnre visualizable tags
        if (contentDeletedEvent.getDeletedTokens().iterator().next().getTagName().equals("line")) {

          int viewPos = getViewPos(contentDeletedEvent.getLocation());
          int viewSpan = 1;

          editor.getText().delete(viewPos, viewPos + viewSpan);

        }

        // Update tag index
        deleteDocTag(contentDeletedEvent.getLocation());
      }


    }

    editor.endBatchEdit();
    editor.addTextChangedListener(this);

  }

  /**
   * After this method is called, changes in EditBox won't be sent to the doc
   * and viceversa. and this binder instance is not usable anymore. EditText
   * text/view is not cleaned up.
   */
  public void unbind() {
    doc.removeListener(this);
    editor.removeTextChangedListener(this);
  }

}
