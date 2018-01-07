/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.client.doodad.selection;

import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.waveprotocol.wave.client.doodad.selection.SelectionAnnotationHandler.STALE_CARET_TIMEOUT_MS;
import static org.waveprotocol.wave.model.document.util.DocCompare.ATTRS;
import static org.waveprotocol.wave.model.document.util.DocCompare.STRUCTURE;

import java.util.HashMap;
import java.util.Map;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.AbstractProfileManager;
import org.waveprotocol.wave.client.account.impl.ProfileImpl;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.doodad.selection.CaretView.CaretViewFactory;
import org.waveprotocol.wave.client.doodad.selection.SelectionAnnotationHandler.CaretListener;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.PainterRegistryImpl;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.RegistriesImpl;
import org.waveprotocol.wave.client.scheduler.testing.FakeTimerService;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.AnnotationRegistryImpl;
import org.waveprotocol.wave.model.document.util.ContextProviders;
import org.waveprotocol.wave.model.document.util.ContextProviders.TestDocumentContext;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.wave.ParticipantId;

import junit.framework.TestCase;

/**
 * Tests for {@link SelectionAnnotationHandler}.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class SelectionAnnotationHandlerTest extends TestCase {

  public static class FakeCaretViewFactory implements CaretViewFactory {
    private final Map<CaretView, Object> markers = new HashMap<CaretView, Object>();

    private final Object dummy = new Object();

    @Override
    public CaretView create() {
      CaretView marker = mock(CaretView.class);
      markers.put(marker, dummy);
      return marker;
    }

    @Override
    public void setMarker(Object element, CaretView marker) {
      assertTrue(markers.containsKey(marker));
      markers.put(marker, element);
    }

    public boolean isAttached(CaretView marker) {
      return markers.get(marker) instanceof Doc.E;
    }
  }

  /** Session for the current editor. */
  class FakeSession {
    final ParticipantId pid;
    final String id;
    final ProfileImpl profile;
    final SelectionExtractor extractor;
    String displayName;
    ProfileManager manager;

    public FakeSession(String name) {

      pid = ParticipantId.ofUnsafe(name + "@example.com");
      id = "#" + name + "#";

      manager = new AbstractProfileManager() {

        @Override
        public String getCurrentSessionId() {
          return id;
        }

        @Override
        public ParticipantId getCurrentParticipantId() {
          return pid;
        }

        @Override
        protected void requestProfile(ParticipantId participantId, RequestProfileCallback callback) {
        }

        @Override
        protected void storeProfile(Profile profile) {
        }
      };

      profile = (ProfileImpl) manager.getProfile(pid);
      extractor = new SelectionExtractor(handlerTimer, manager);
      displayName = profile.getName();
    }

    void setName(String newName) {
      ProfileImpl p = (ProfileImpl) me.manager.getProfile(pid);
      p.setName(newName);
    }
  }

  AnnotationRegistry annotationRegistry = new AnnotationRegistryImpl();
  FakeTimerService painterTimer = new FakeTimerService();
  FakeTimerService handlerTimer = new FakeTimerService();
  {
    handlerTimer.setStartTime(painterTimer.currentTimeMillis());
  }
  PainterRegistry painterRegistry =
      new PainterRegistryImpl("s", "b", new AnnotationPainter(painterTimer));
  FakeSession me = new FakeSession("me");
  FakeSession me2 = new FakeSession("me2");
  FakeSession joe = new FakeSession("joe");
  FakeCaretViewFactory markerFactory = new FakeCaretViewFactory();

  TestDocumentContext<Node, Element, Text> cxt;

  private SelectionAnnotationHandler handler;

  @Override
  protected void setUp() {
    Registries registries =
        new RegistriesImpl(mock(ElementHandlerRegistry.class), annotationRegistry, painterRegistry);
    handler = SelectionAnnotationHandler.register(
        registries, handlerTimer, markerFactory, me.id, me.manager, new CaretListener() {

          @Override
          public void onActive(String address, RgbColor color) {
            // no op
          }

          @Override
          public void onExpire(String address) {
            // no op
          }

        });

    String content = "abcdefghij";
    cxt = ContextProviders.createTestPojoContext2(
        content, null, annotationRegistry, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    AnnotationPainter.createAndSetDocPainter(cxt, painterRegistry);
  }

  public void testBehaviour() {
    // Ignore own cursor
    me.extractor.writeSelection(
        cxt.document(), new FocusedRange(2, 4), "wo", handlerTimer.currentTimeMillis());
    assertTrue(markerFactory.markers.isEmpty());
    check("abcdefghij");

    joe.extractor.writeSelection(
        cxt.document(), new FocusedRange(1, 3), "ni", handlerTimer.currentTimeMillis());
    assertFalse(markerFactory.markers.isEmpty());
    CaretView joeUi = handler.getUiForSession(joe.id);
    verify(joeUi).setName("Joe");
    verify(joeUi).setCompositionState("ni");
    assertFalse(markerFactory.isAttached(joeUi));
    painterTimer.tick(10);
    assertTrue(markerFactory.isAttached(joeUi));
    check("a<s>bc</s><b/>defghij");

    joe.setName("Jimbo");
    verify(joeUi).setName("Jimbo");
    assertEquals(2, painterRegistry.getKeys().countEntries());

    handlerTimer.tick(STALE_CARET_TIMEOUT_MS / 2);
    joe.setName("Jimbo2");
    verify(joeUi, atMost(1)).setName("Jimbo2");

    // check cleanup after timeout
    handlerTimer.tick(STALE_CARET_TIMEOUT_MS);
    joe.setName("Jimbo3");
    verify(joeUi, never()).setName("Jimbo3");
    assertEquals(0, painterRegistry.getKeys().countEntries());

    joe.extractor.writeSelection(
        cxt.document(), new FocusedRange(2, 4), "ni", handlerTimer.currentTimeMillis());
    handlerTimer.tick(STALE_CARET_TIMEOUT_MS - 1);
    painterTimer.tick(10);
    joe.setName("Jimbo4");
    verify(joeUi).setName("Jimbo4");

    joe.extractor.writeSelection(
        cxt.document(), new FocusedRange(2, 4), "ni", handlerTimer.currentTimeMillis());
    handlerTimer.tick(STALE_CARET_TIMEOUT_MS - 1);
    painterTimer.tick(10);
    joe.setName("Jimbo5");
    verify(joeUi).setName("Jimbo5");
  }

  private void check(String xml) {
    assertTrue(DocCompare.equivalent(STRUCTURE - ATTRS,
        xml.replaceAll("<b/>", "<b><" + CaretMarkerRenderer.FULL_TAGNAME + "/></b>"),
        cxt.annotatableContent()));
  }

}
