package org.waveprotocol.mod.model.p2pvalue.docbased;

import org.waveprotocol.mod.model.p2pvalue.ModelIndex;
import org.waveprotocol.mod.model.p2pvalue.Project;
import org.waveprotocol.mod.model.p2pvalue.Task;
import org.waveprotocol.mod.model.p2pvalue.Task.Initialiser;
import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.Collections;

/**
 * A Document-based Project
 *
 * <project name="..." status="..." description="..."> <tasks> <!-- here a
 * ObservableList<Task>--> </tasks> </project>
 *
 * @author pablojan@gmail.com
 *
 */
public class DocBasedProject implements Project {


  public static final String DOC_ID_PREFIX = "prj";

  // Doc Top tag <project ...>
  public static final String TOP_TAG = "project";


  // Name <project name="...">
  private static final String NAME_ATTR = "name";
  private final BasicValue<String> name;

  // Status <project status="...">
  private static final String STATUS_ATTR = "status";
  private final ObservableBasicValue<String> status;
  private final ObservableBasicValue.Listener<String> statusListener;

  // Description <project description="...">
  private static final String DESCRIPTION_ATTR = "description";
  private final BasicValue<String> description;

  // Tasks <tasks>
  private static final String TASKS_TAG = "tasks";
  private final ObservableElementList<Task, Task.Initialiser> tasks;
  private final ObservableElementList.Listener<Task> tasksListener;


  // Listeners
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private ModelIndex modelIndex;
  private String documentId;

  /**
   * Creates a DocBasedProject instance backed by a Document. The top element
   * must already exist.
   *
   * @param router Document
   * @param top the parent Element of the Project inside the document
   * @return a DocBasedProject instance
   */
  private static <E> DocBasedProject create(DocumentEventRouter<? super E, E, ?> router,
      E projectElement, E tasksElement) {

    Preconditions.checkArgument(router.getDocument().getTagName(projectElement).equals(TOP_TAG),
        "Invalid Project top tag %s", router.getDocument().getTagName(projectElement));

    return new DocBasedProject(DocumentBasedBasicValue.create(router, projectElement,
        Serializer.STRING, NAME_ATTR), DocumentBasedBasicValue.create(router, projectElement,
        Serializer.STRING, STATUS_ATTR), DocumentBasedBasicValue.create(router, projectElement,
        Serializer.STRING, DESCRIPTION_ATTR), DocumentBasedElementList.create(router, tasksElement,
        DocBasedTask.TOP_TAG, DocBasedTask.<E> factory()));
  }


  /**
   * Create or adapt a DocBasedProject backed by the provided Wavelet. This
   * method is provided because Project is the root type of the Document.
   * 
   * @param doc Document supporting the Project
   * @return
   */
  public static <E> DocBasedProject create(ObservableDocument doc) {

    DocEventRouter router = DefaultDocEventRouter.create(doc);

    // <project>
    Doc.E projectElement = DocHelper.getElementWithTagName(doc, TOP_TAG);
    if (projectElement == null) {
      doc.createChildElement(doc.getDocumentElement(), TOP_TAG,
          Collections.<String, String> emptyMap());
    }

    // <tasks>
    Doc.E tasksElement = DocHelper.getElementWithTagName(doc, TASKS_TAG);
    if (tasksElement == null) {
      doc.createChildElement(doc.getDocumentElement(), TASKS_TAG,
          Collections.<String, String> emptyMap());
    }

    return create(router, projectElement, tasksElement);

  }

  // Constructor

  DocBasedProject(BasicValue<String> name, ObservableBasicValue<String> status,
      BasicValue<String> description, ObservableElementList<Task, Task.Initialiser> tasks) {

    // Set Attributes

    this.name = name;
    this.status = status;
    this.description = description;
    this.tasks = tasks;

    // Define Listeners

    this.statusListener = new ObservableBasicValue.Listener<String>() {

      @Override
      public void onValueChanged(String oldValue, String newValue) {
        for (Listener l : listeners)
          l.onStatusChanged(newValue);
      }
    };

    this.tasksListener = new ObservableElementList.Listener<Task>() {

      @Override
      public void onValueAdded(Task entry) {
        for (Listener l : listeners)
          l.onTaskAdded(entry);
      }

      @Override
      public void onValueRemoved(Task entry) {
        for (Listener l : listeners)
          l.onTaskRemoved(entry);

      }
    };

  }


  // TODO this should be protected
  public void setIndexMetadata(String documentId, ModelIndex modelIndex) {
    this.modelIndex = modelIndex;
    this.documentId = documentId;
  }


  @Override
  public void setName(String name) {
    this.name.set(name);
  }



  @Override
  public String getName() {
    return name.get();
  }


  @Override
  public void setStatus(String status) {
    this.status.set(status);
  }


  @Override
  public String getStatus() {
    return status.get();
  }


  @Override
  public void setDescription(String description) {
    this.description.set(description);
  }


  @Override
  public String getDescription() {
    return description.get();
  }



  @Override
  public String getDocumentId() {
    return documentId;
  }


  @Override
  public int getNumTasks() {
    return tasks.size();
  }


  @Override
  public Iterable<Task> getTasks() {
    return tasks.getValues();
  }


  @Override
  public Task addTask(Initialiser task) {
    return tasks.add(task);
  }


  @Override
  public void removeTask(Task task) {
    tasks.remove(task);
  }

  @Override
  public Task getTask(int index) {
    return tasks.get(index);
  }


  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }


  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

}
