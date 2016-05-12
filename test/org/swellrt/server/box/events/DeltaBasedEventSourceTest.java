package org.swellrt.server.box.events;

import junit.framework.TestCase;

import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletConfigurator;
import org.waveprotocol.wave.model.wave.opbased.WaveViewImpl.WaveletFactory;
import org.waveprotocol.wave.util.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeltaBasedEventSourceTest extends TestCase {

  private static final Log LOG = Log.get(DeltaBasedEventSourceTest.class);

  static String fakeDomain = "example.com";
  static ParticipantId fakeAuthor = ParticipantId.ofUnsafe("nobody@example.com");
  static IdGenerator fakeIdGenerator = FakeIdGenerator.create();
  static ObservableWaveView fakeWave = null;
  static Model fakeDataModel = null;

  public static interface EventAssertionChecker {

    public void checkAssertions(Event e);

  }

  /**
   * A sink to collect wavelet operations after data model changes. It keeps ops
   * in a buffer until the #getTransformedWaveletDelta() is called.
   */
  private static class BufferedSilentOperationSink implements SilentOperationSink<WaveletOperation> {

    private List<WaveletOperation> opBuffer = new ArrayList<WaveletOperation>();
    private int version = 0;


    /**
     * Get buffered ops as a transformed wavelet delta and reset the buffer.
     *
     * @return
     */
    public TransformedWaveletDelta getTransformedWaveletDelta() {
      TransformedWaveletDelta deltas =
          TransformedWaveletDelta.cloneOperations(fakeAuthor, HashedVersion.unsigned(++version),
              System.currentTimeMillis(), opBuffer);
      opBuffer.clear();
      return deltas;
    }


    @Override
    public void consume(WaveletOperation op) {
      opBuffer.add(op);
    }

  }

  /**
   * Create an sample wave view with a op-based wavelet factory
   *
   * @return
   */
  private static ObservableWaveView createSampleWave(
      SilentOperationSink<? super WaveletOperation> sink) {

    final WaveViewDataImpl waveData = WaveViewDataImpl.create(fakeIdGenerator.newWaveId());
    final DocumentFactory<?> docFactory = BasicFactories.fakeDocumentFactory();
    final ObservableWaveletData.Factory<?> waveletDataFactory =
        new ObservableWaveletData.Factory<WaveletDataImpl>() {
          private final ObservableWaveletData.Factory<WaveletDataImpl> inner =
              WaveletDataImpl.Factory.create(docFactory);

          @Override
          public WaveletDataImpl create(ReadableWaveletData data) {
            WaveletDataImpl wavelet = inner.create(data);
            waveData.addWavelet(wavelet);
            return wavelet;
          }
        };
    WaveletFactory<OpBasedWavelet> waveletFactory =
        BasicFactories.opBasedWaveletFactoryBuilder().with(fakeAuthor).with(waveletDataFactory)
            .with(sink)
            .build();

    WaveViewImpl<?> wave =
        WaveViewImpl.create(waveletFactory, waveData.getWaveId(), fakeIdGenerator, fakeAuthor,
            WaveletConfigurator.ADD_CREATOR);

    return wave;

  }

  /**
   * A sink to collect wavelet operations after data model changes. It keeps ops
   * in a buffer until the #getTransformedWaveletDelta() is called.
   */
  private static BufferedSilentOperationSink fakeSink =
      new BufferedSilentOperationSink();

  /**
   * Initialize a shared fake wave for all tests. Data model init data should be
   * added here.
   */
  static {
    fakeWave = createSampleWave(fakeSink);
    fakeDataModel = Model.create(fakeWave, fakeDomain, fakeAuthor, Boolean.TRUE, fakeIdGenerator);
    fakeDataModel.getRoot().put("one", fakeDataModel.createString("String One"));
    ListType list =
        (ListType) fakeDataModel.getRoot().put("list", fakeDataModel.createList()).asList();
    list.add(fakeDataModel.createString("list-zero"));
    list.add(fakeDataModel.createString("list-one"));
    list.add(fakeDataModel.createString("list-two"));
    MapType map = (MapType) list.add(fakeDataModel.createMap()).asMap();
    map.put("l4-k1", fakeDataModel.createString("value 1"));
    map.put("l4-k2", fakeDataModel.createString("value 2"));

    fakeDataModel.getRoot().put("two", fakeDataModel.createString("String Two"));
    fakeDataModel.getRoot().put("map", fakeDataModel.createMap());
    fakeDataModel.getRoot().put("text", fakeDataModel.createText());
    LOG.fine("Created fake Wave and Data Model");
  } // root.list.?.<property>



  protected void setUp() throws Exception {
    super.setUp();
    // Emtpy the buffer of operations.
    fakeSink.getTransformedWaveletDelta();
    LOG.fine("Emtpy sink buffered operations");
  }


  /**
   * An utility method wiring all componentes of these tests: EventQueue,
   * EventSource, EventSourceConfigurator...
   *
   * @param pathsToExtract
   * @param _app
   * @param _dataType
   * @param assertions
   * @throws InvalidIdException
   */
  private void testWaveletUpdate(final ArrayList<String> pathsToExtract, final String _app,
      final String _dataType, final EventAssertionChecker assertions) throws InvalidIdException {

    EventQueue eventQueue = new EventQueue() {
      @Override
      public void add(Event event) {
        assertions.checkAssertions(event);
      }

      @Override
      public boolean hasEventsFor(String app, String dataType) {
        return (_app == null && _dataType == null)
            || (_app.equals(app) && _dataType.equals(dataType));
      }

      @Override
      public Set<String> getExpressionPaths(String app, String dataType) {
        return new HashSet<String>(pathsToExtract);
      }

      @Override
      public void registerListener(EventQueueListener listener) {
        // TODO Auto-generated method stub

      }

      @Override
      public void registerConfigurator(EventQueueConfigurator configurator) {
        // TODO Auto-generated method stub

      }

    };

    DeltaBasedEventSource eventSource = new DeltaBasedEventSource(eventQueue);

    eventSource.waveletUpdate(
        fakeWave.getWavelet(
            ModernIdSerialiser.INSTANCE.deserialiseWaveletId(fakeDomain + "/"
                + Model.WAVELET_SWELL_ROOT))
            .getWaveletData(), DeltaSequence.of(fakeSink.getTransformedWaveletDelta()));

  }


  public void testEventListItemAddedString() throws InvalidIdException {


    // Mutate the fake data model. Changes reach the sink as doc ops.
    ListType list = (ListType) fakeDataModel.getRoot().get("list").asList();
    list.add(fakeDataModel.createString("Hello World"));


    // Generate event and test
    testWaveletUpdate(CollectionUtils.<String> newArrayList(), "default", "default",
        new EventAssertionChecker() {

      @Override
      public void checkAssertions(Event e) {

        assertEquals(Event.Type.LIST_ITEM_ADDED, e.getType());
        assertEquals("Hello World", e.getContextData().get("root.list.?"));

      }
    });

  }

  //
  // LIST_ITEM_ADDED
  //
  public void testEventListItemAddedMap() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    ListType list = (ListType) fakeDataModel.getRoot().get("list").asList();

    // Operation (Step 0)
    MapType map = (MapType) list.add(fakeDataModel.createMap());

    map.put("field1", "foo");
    map.put("field2", "bar");

    MapType map_map = (MapType) map.put("map", fakeDataModel.createMap());
    map_map.put("field1", "boom");
    map_map.put("field2", "flash");


    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList(
        "root.one",
        "root.two",
        "root.list.?.field1",
        "root.list.?.field2",
        "root.list.?.map.field1",
        "root.list.?.map.field2"),
        "default", "default", new EventAssertionChecker() {

      int opStep = 0;

      @Override
      public void checkAssertions(Event e) {

        LOG.fine(e.getType().toString());

        // We test only first model change
        if (opStep == 0) {

          assertEquals(Event.Type.LIST_ITEM_ADDED, e.getType());

          // Chech event-specific context data

          // Simple values in the list
          assertEquals("foo", e.getContextData().get("root.list.?.field1"));
          assertEquals("bar", e.getContextData().get("root.list.?.field2"));

          // Map values in the list item
          assertEquals("boom", e.getContextData().get("root.list.?.map.field1"));
          assertEquals("flash", e.getContextData().get("root.list.?.map.field2"));

          opStep++;
        }

        // Check generic context data, it applies for all events
        assertEquals("String One", e.getContextData().get("root.one"));
        assertEquals("String Two", e.getContextData().get("root.two"));

      }
    });

  }


  //
  // LIST_ITEM_REMOVED
  //
  public void testEventListItemRemoved() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    ListType list = (ListType) fakeDataModel.getRoot().get("list").asList();
    list.add(fakeDataModel.createString("item one"));
    list.add(fakeDataModel.createString("item two"));
    list.add(fakeDataModel.createMap());
    list.add(fakeDataModel.createList());

    // Clear sink operations so far
    fakeSink.getTransformedWaveletDelta();

    // Delete these items
    list.remove(list.size() - 4);
    list.remove(list.size() - 3);
    list.remove(list.size() - 2);
    list.remove(list.size() - 1);



    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList("root.one", "root.two"), "default",
        "default", new EventAssertionChecker() {


      @Override
      public void checkAssertions(Event e) {

            LOG.fine(e.getType().toString());

            assertEquals(Event.Type.LIST_ITEM_REMOVED, e.getType());

            // Check generic context data, it applies for all events
            assertEquals("String One", e.getContextData().get("root.one"));
            assertEquals("String Two", e.getContextData().get("root.two"));

      }
    });

  }

  //
  // MAP_ENTRY_UPDATED
  //
  public void testEventMapEntryUpdated() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    MapType map = (MapType) fakeDataModel.getRoot().get("map").asMap();
    map.put("field0", "hello world");

    // Reset operations sink
    fakeSink.getTransformedWaveletDelta();

    // Three updates in the same delta, all of them will share the same last
    // state = "ro"
    map.put("field1", "foo");
    map.put("field1", "bar");
    map.put("field1", "ro");



    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList(
        "root.one",
        "root.two",
        "root.map.field0",
        "root.map.field1",
        "root.list.?.field1")
        , "default", "default", new EventAssertionChecker() {

      @Override
      public void checkAssertions(Event e) {

            LOG.fine(e.getType().toString());

            // All updates has the same value (the lastest)
            assertEquals(Event.Type.MAP_ENTRY_UPDATED, e.getType());
            assertEquals("ro", e.getContextData().get("root.map.field1"));

            assertEquals("hello world", e.getContextData().get("root.map.field0"));
            assertNull(e.getContextData().get("root.list.?.field1"));

            // Check generic context data, it applies for all events
            assertEquals("String One", e.getContextData().get("root.one"));
            assertEquals("String Two", e.getContextData().get("root.two"));

        }
    });

  }

  //
  // MAP_ENTRY_UPDATED inside a LIST
  //
  public void testEventMapEntryUpdatedInsideList() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    MapType map = ((MapType) fakeDataModel.getRoot().get("list").asList().get(3)).asMap();
    map.put("l4-k1", "hello world");

    // Reset operations sink
    fakeSink.getTransformedWaveletDelta();

    // Three updates in the same delta, all of them will share the same last
    // state = "ro"
    map.put("l4-k2", "foo");
    map.put("l4-k2", "bar");
    map.put("l4-k2", "ro");


    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList(
        "root.one",
        "root.two",
        "root.map.field0",
        "root.map.field1",
        "root.list.?.l4-k1",
        "root.list.?.l4-k2"), "default", "default",
        new EventAssertionChecker() {

          @Override
          public void checkAssertions(Event e) {

            LOG.fine(e.getType().toString());

            // All updates has the same value (the lastest)
            assertEquals(Event.Type.MAP_ENTRY_UPDATED, e.getType());
            assertEquals("ro", e.getContextData().get("root.list.?.l4-k2"));

            assertEquals("hello world", e.getContextData().get("root.list.?.l4-k1"));

            // Check generic context data, it applies for all events
            assertEquals("String One", e.getContextData().get("root.one"));
            assertEquals("String Two", e.getContextData().get("root.two"));

          }
        });

  }


  //
  // MAP_ENTRY_REMOVED
  //
  public void testEventMapEntryRemoved() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    MapType map = (MapType) fakeDataModel.getRoot().get("map").asMap();
    map.put("field2", "delete me");
    map.put("field3", "update me");

    // Reset operations sink
    fakeSink.getTransformedWaveletDelta();

    // Mix updates and removes to check right doc op processing
    map.put("field2", "please, delete me");
    map.put("field3", "i am updated");
    map.remove("field2");


    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList(
        "root.one",
        "root.two",
        "root.map.field2", "root.map.field3"),
        "default", "default",
        new EventAssertionChecker() {

          int step = 0;

          @Override
          public void checkAssertions(Event e) {

            LOG.fine(e.getType().toString());

            if (step == 0) {
              assertEquals(Event.Type.MAP_ENTRY_UPDATED, e.getType());
              assertEquals("please, delete me", e.getContextData().get("root.map.field2"));
              step++;
            }

            if (step == 1) {
              assertEquals(Event.Type.MAP_ENTRY_UPDATED, e.getType());
              assertEquals("i am updated", e.getContextData().get("root.map.field3"));
              step++;
            }

            if (step == 3) {
              assertEquals(Event.Type.MAP_ENTRY_REMOVED, e.getType());
              assertNull(e.getContextData().get("root.map.field2"));
              step++;
            }

            // Check generic context data, it applies for all events
            assertEquals("String One", e.getContextData().get("root.one"));
            assertEquals("String Two", e.getContextData().get("root.two"));

          }
        });

  }


  //
  // ADD_PARTICIPANT
  //
  public void testEventAddParticipant() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    fakeDataModel.addParticipant("peter@" + fakeDomain);


    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList("root.one", "root.two"), "default",
        "default", new EventAssertionChecker() {

          @Override
          public void checkAssertions(Event e) {

            LOG.fine(e.getType().toString());

            assertEquals(Event.Type.ADD_PARTICIPANT, e.getType());
            assertEquals("peter@" + fakeDomain, e.getParticipant());

            // Check generic context data, it applies for all events
            assertEquals("String One", e.getContextData().get("root.one"));
            assertEquals("String Two", e.getContextData().get("root.two"));
          }
        });

  }

  //
  // REMOVE_PARTICIPANT
  //
  public void testEventRemoveParticipant() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    fakeDataModel.removeParticipant("peter@" + fakeDomain);


    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList(
        "root.one",
        "root.two"),
        "default",
        "default", new EventAssertionChecker() {

          @Override
          public void checkAssertions(Event e) {

            LOG.fine(e.getType().toString());

            assertEquals(Event.Type.REMOVE_PARTICIPANT, e.getType());
            assertEquals("peter@" + fakeDomain, e.getParticipant());

            // Check generic context data, it applies for all events
            assertEquals("String One", e.getContextData().get("root.one"));
            assertEquals("String Two", e.getContextData().get("root.two"));
          }
        });

  }

  //
  // DOC_CHANGE
  //
  public void testEventDocChange() throws InvalidIdException {

    // Mutate the fake data model. Changes reach the sink as doc ops.
    TextType text = (TextType) fakeDataModel.fromPath("root.text");

    text.insertText(text.getSize() - 1, "document fever");

    // Test generated events
    testWaveletUpdate(CollectionUtils.<String> newArrayList("root.one", "root.two"), "default",
        "default", new EventAssertionChecker() {

          @Override
          public void checkAssertions(Event e) {

            LOG.fine(e.getType().toString());

            assertEquals(Event.Type.DOC_CHANGE, e.getType());
            assertEquals("document fever", e.getCharacters());

            // Check generic context data, it applies for all events
            assertEquals("String One", e.getContextData().get("root.one"));
            assertEquals("String Two", e.getContextData().get("root.two"));
          }
        });

  }

}

