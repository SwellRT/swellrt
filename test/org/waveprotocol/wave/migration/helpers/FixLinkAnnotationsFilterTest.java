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

package org.waveprotocol.wave.migration.helpers;

import static org.waveprotocol.wave.migration.helpers.FixLinkAnnotationsFilter.AUTO;
import static org.waveprotocol.wave.migration.helpers.FixLinkAnnotationsFilter.NEW;
import static org.waveprotocol.wave.migration.helpers.FixLinkAnnotationsFilter.OLD_MANUAL;
import static org.waveprotocol.wave.migration.helpers.FixLinkAnnotationsFilter.OLD_WAVE;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class FixLinkAnnotationsFilterTest extends TestCase {
  IndexedDocument<Node, Element, Text> doc = DocProviders.POJO.parse("12345678901234567890");
  IndexedDocument<Node, Element, Text> checkDoc = DocProviders.POJO.parse("12345678901234567890");
  String waveid = "example.com!foobar";
  String waveref = "example.com/foobar";
  String webHref = "http://example.com";
  String waveHref = "wave://example.com/foobar";
  Nindo.Builder fb;
  FixLinkAnnotationsFilter f;
  Nindo.Builder cb;

  @Override
  protected void setUp() {
    newBuilders();
  }

  public void testRenamesLinkManual() throws OperationException {
    f.skip(1);
    f.startAnnotation(OLD_MANUAL, webHref);
    f.skip(2);
    f.endAnnotation(OLD_MANUAL);

    cb.skip(1);
    cb.startAnnotation(NEW, webHref);
    cb.skip(2);
    cb.endAnnotation(NEW);

    checkEqual();
  }

  public void testReplacesLinkWave() throws OperationException {
    f.skip(1);
    f.startAnnotation(OLD_WAVE, waveid);
    f.skip(2);
    f.endAnnotation(OLD_WAVE);

    cb.skip(1);
    cb.startAnnotation(NEW, waveHref);
    cb.skip(2);
    cb.endAnnotation(NEW);

    checkEqual();
  }

  public void testReplacesLinkWaveContainingWaveref() throws OperationException {
    f.skip(1);
    f.startAnnotation(OLD_WAVE, waveref);
    f.skip(2);
    f.endAnnotation(OLD_WAVE);

    cb.skip(1);
    cb.startAnnotation(NEW, waveHref);
    cb.skip(2);
    cb.endAnnotation(NEW);

    checkEqual();
  }

  public void testManualLinkWinsWhenStacked() throws OperationException {
    f.skip(1);
    f.startAnnotation(OLD_WAVE, waveid);
    f.startAnnotation(OLD_MANUAL, webHref);
    f.skip(2);
    f.endAnnotation(OLD_WAVE);
    f.endAnnotation(OLD_MANUAL);
    f.skip(1);
    // Now in the other order
    f.startAnnotation(OLD_MANUAL, webHref);
    f.startAnnotation(OLD_WAVE, waveid);
    f.skip(2);
    f.endAnnotation(OLD_MANUAL);
    f.endAnnotation(OLD_WAVE);

    cb.skip(1);
    cb.startAnnotation(NEW, webHref);
    cb.skip(2);
    cb.endAnnotation(NEW);
    cb.skip(1);
    cb.startAnnotation(NEW, webHref);
    cb.skip(2);
    cb.endAnnotation(NEW);

    checkEqual();
  }

  public void testManualLinkWinsWhenOverlapping() throws OperationException {
    f.skip(1);
    f.startAnnotation(OLD_WAVE, waveid);
    f.skip(1);
    f.startAnnotation(OLD_MANUAL, webHref);
    f.skip(1);
    f.endAnnotation(OLD_WAVE);
    f.skip(1);
    f.endAnnotation(OLD_MANUAL);

    cb.skip(1);
    cb.startAnnotation(NEW, waveHref);
    cb.skip(1);
    cb.startAnnotation(NEW, webHref);
    cb.skip(2);
    cb.endAnnotation(NEW);

    checkEqual();


    newBuilders();

    f.skip(1);
    f.startAnnotation(OLD_MANUAL, webHref);
    f.skip(1);
    f.startAnnotation(OLD_WAVE, waveid);
    f.skip(1);
    f.endAnnotation(OLD_MANUAL);
    f.skip(1);
    f.endAnnotation(OLD_WAVE);

    cb.skip(1);
    cb.startAnnotation(NEW, webHref);
    cb.skip(2);
    cb.startAnnotation(NEW, waveHref);
    cb.skip(1);
    cb.endAnnotation(NEW);

    checkEqual();


    newBuilders();

    f.startAnnotation(OLD_MANUAL, webHref);
    f.skip(1);
    f.endAnnotation(OLD_MANUAL);
    f.startAnnotation(OLD_WAVE, waveid);
    f.skip(1);
    f.endAnnotation(OLD_WAVE);
    f.startAnnotation(OLD_MANUAL, webHref);
    f.skip(1);
    f.startAnnotation(OLD_WAVE, waveid);
    f.endAnnotation(OLD_MANUAL);
    f.skip(1);
    f.startAnnotation(OLD_MANUAL, webHref);
    f.endAnnotation(OLD_WAVE);
    f.skip(1);
    f.endAnnotation(OLD_MANUAL);

    cb.startAnnotation(NEW, webHref);
    cb.skip(1);
    cb.startAnnotation(NEW, waveHref);
    cb.skip(1);
    cb.startAnnotation(NEW, webHref);
    cb.skip(1);
    cb.startAnnotation(NEW, waveHref);
    cb.skip(1);
    cb.startAnnotation(NEW, webHref);
    cb.skip(1);
    cb.endAnnotation(NEW);

    checkEqual();
  }

  public void testNormalizesValues() throws OperationException {
    f.skip(1);
    f.startAnnotation(OLD_MANUAL, "waveid://" + waveid);
    f.skip(2);
    f.endAnnotation(OLD_MANUAL);
    f.skip(1);
    f.startAnnotation(AUTO, "waveid://" + waveid);
    f.skip(2);
    f.endAnnotation(AUTO);

    cb.skip(1);
    cb.startAnnotation(NEW, waveHref);
    cb.skip(2);
    cb.endAnnotation(NEW);
    cb.skip(1);
    cb.startAnnotation(AUTO, waveHref);
    cb.skip(2);
    cb.endAnnotation(AUTO);

    checkEqual();
  }

  public void testIgnoresOtherAnnotations() throws OperationException {
    f.skip(1);
    f.startAnnotation("foo", "bar");
    f.skip(2);
    f.endAnnotation("foo");

    cb.skip(1);
    cb.startAnnotation("foo", "bar");
    cb.skip(2);
    cb.endAnnotation("foo");

    checkEqual();
  }

  void newBuilders() {
    fb = Nindo.builder();
    f = new FixLinkAnnotationsFilter(fb);
    cb = Nindo.builder();
    f.begin();
    cb.begin();
  }

  void checkEqual() throws OperationException {
    cb.finish();
    f.finish();
    Nindo filteredNindo = fb.build();
    checkDoc.consumeAndReturnInvertible(cb.build());
    doc.consumeAndReturnInvertible(filteredNindo);
    assertEquals(DocOpUtil.toXmlString(checkDoc.asOperation()),
        DocOpUtil.toXmlString(doc.asOperation()));
  }
}
