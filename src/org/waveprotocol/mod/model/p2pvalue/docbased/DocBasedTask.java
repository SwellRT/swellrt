package org.waveprotocol.mod.model.p2pvalue.docbased;

import org.waveprotocol.mod.model.p2pvalue.Reminder;
import org.waveprotocol.mod.model.p2pvalue.Task;
import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableBasicSet;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicSet;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.Map;

public class DocBasedTask implements Task {


  // Top tag <task>
  public static final String TOP_TAG = "task";

  // Name <task name="...">
  private static final String NAME_ATTR = "name";
  private final BasicValue<String> name;

  // Status <task status="...">
  private static final String STATUS_ATTR = "status";
  private final ObservableBasicValue<String> status;
  private final ObservableBasicValue.Listener<String> statusListener;

  // Description <task description="...">
  private static final String DESCRIPTION_ATTR = "description";
  private final BasicValue<String> description;

  // Description <task deadline="...">
  private static final String DEADLINE_ATTR = "dl";
  private final ObservableBasicValue<Long> deadline;
  private final ObservableBasicValue.Listener<Long> deadlineListener;


  // Participants <participants><participant id="" />...</participants>
  private static final String PARTICIPANTS_TAG = "participants";
  private static final String PARTICIPANT_TAG = "participant";
  private static final String PARTICIPANT_ID_ATTR = "id";
  private final ObservableBasicSet<String> participants;
  private final ObservableBasicSet.Listener<String> participantsListener;


  // Listeners
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();



  protected static <E> Factory<E, Task, Task.Initialiser> factory() {

    return new Factory<E, Task, Task.Initialiser>() {

        @Override
      public DocBasedTask adapt(DocumentEventRouter<? super E, E, ?> router, E taskElement) {
        return create(router, taskElement);
        }

        @Override
      public Initializer createInitializer(final Initialiser initialState) {

        return new Initializer() {

          @Override
          public void initialize(Map<String, String> target) {
            target.put(NAME_ATTR, initialState.name);
          }

        };
        }

      };
  }


  /**
   * Create a DocBasedTask instance. The taskElement must already exist.
   *
   * @param router Document
   * @param taskElement the document element where this instance is attached to
   * @return DocBasedTask instance
   */
  private static <E> DocBasedTask create(DocumentEventRouter<? super E, E, ?> router, E taskElement) {

    Preconditions.checkArgument(router.getDocument().getTagName(taskElement).equals(TOP_TAG),
        "Invalid Task top tag %s", router.getDocument().getTagName(taskElement));

    //
    // Check sub tag <participants>
    // <task name="..." description="..." ...>
    // <participants>
    // <participant id="..."></paricipant>
    // <participant id="..."></paricipant>
    // ...
    // </participants>
    // </task>

    E participantsElement =
        DocHelper.getElementWithTagName(router.getDocument(), PARTICIPANTS_TAG, taskElement);

    if (participantsElement == null)
      router.getDocument().createChildElement(taskElement, PARTICIPANTS_TAG,
            Collections.<String, String> emptyMap());


    return new DocBasedTask(DocumentBasedBasicValue.create(router, taskElement, Serializer.STRING,
        NAME_ATTR), DocumentBasedBasicValue.create(router, taskElement, Serializer.STRING,
        STATUS_ATTR), DocumentBasedBasicValue.create(router, taskElement, Serializer.STRING,
        DESCRIPTION_ATTR), DocumentBasedBasicValue.create(router, taskElement, Serializer.LONG,
        DEADLINE_ATTR), DocumentBasedBasicSet.create(router, participantsElement,
        Serializer.STRING, PARTICIPANT_TAG, PARTICIPANT_ID_ATTR));

  }


  // Constructor

  DocBasedTask(BasicValue<String> name, ObservableBasicValue<String> status,
      BasicValue<String> description, ObservableBasicValue<Long> deadline,
      ObservableBasicSet<String> participants) {

    // Set attributes

    this.name = name;
    this.status = status;
    this.description = description;
    this.deadline = deadline;
    this.participants = participants;

    // Define listeners

    this.statusListener = new ObservableBasicValue.Listener<String>() {

      @Override
      public void onValueChanged(String oldValue, String newValue) {
        for (Listener l : listeners)
          l.onStatusChanged(newValue);
      }
    };

    this.deadlineListener = new ObservableBasicValue.Listener<Long>() {

      @Override
      public void onValueChanged(Long oldValue, Long newValue) {
        for (Listener l : listeners)
          l.onDeadlineChanged(newValue);
      }
    };

    this.participantsListener = new ObservableBasicSet.Listener<String>() {

      @Override
      public void onValueAdded(String newValue) {
        for (Listener l : listeners)
          l.onParticipantAdded(ParticipantId.ofUnsafe(newValue));
      }

      @Override
      public void onValueRemoved(String oldValue) {
        for (Listener l : listeners)
          l.onParticipantRemoved(ParticipantId.ofUnsafe(oldValue));
      }


    };

  }


  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
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
  public void addParticipant(String participant) {
    participants.add(participant);
  }

  @Override
  public void removeParticipant(String participant) {
    participants.remove(participant);
  }

  @Override
  public Iterable<String> getParticipants() {
    return participants.getValues();
  }

  @Override
  public boolean isParticipantInTask(String participant) {
    return participants.contains(participant);
  }

  @Override
  public void setDeadline(long datetime) {
    this.deadline.set(Long.valueOf(datetime));
  }

  @Override
  public long getDeadline() {
    return deadline.get().longValue();
  }

  @Override
  public void addReminder(ParticipantId participant, long datetime) {
    // TODO Complete
  }

  @Override
  public void removeReminder(Reminder reminder) {
    // TODO Complete
  }

  @Override
  public Reminder[] getReminders(ParticipantId participant) {
    // TODO Complete
    return null;
  }

}
