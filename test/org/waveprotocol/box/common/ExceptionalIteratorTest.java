// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.box.common;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for the exceptional iterator.
 *
 * @author anorth@google.com (Alex North)
 */
public class ExceptionalIteratorTest extends TestCase {

  private static final List<Integer> items = Arrays.asList(1, 2, 3);

  public void testEmptyIteratorIsEmpty() throws Exception {
    ExceptionalIterator<?, Exception> empty = ExceptionalIterator.Empty.create();
    assertFalse(empty.hasNext());
  }

  public void testFailingIteratorFails() {
    Exception ex = new Exception("for testing");
    ExceptionalIterator<?, Exception> failing = ExceptionalIterator.Failing.create(ex);
    try {
      failing.hasNext();
      fail("Expected an exception");
    } catch (Exception e) {
      assertSame(ex, e);
    }
  }

  public void testFromIteratorNoException() throws Exception {
    ExceptionalIterator<Integer, Exception> itr =
        ExceptionalIterator.FromIterator.create(items.iterator());
    assertTrue(itr.hasNext());
    assertEquals(Integer.valueOf(1), itr.next());
    assertEquals(Integer.valueOf(2), itr.next());
    assertEquals(Integer.valueOf(3), itr.next());
    assertFalse(itr.hasNext());
  }

  public void testFromIteratorWithException() throws Exception {
    Exception ex = new Exception("for testing");

    ExceptionalIterator<Integer, Exception> itr =
        ExceptionalIterator.FromIterator.create(items.iterator(), ex);
    assertTrue(itr.hasNext());
    assertEquals(Integer.valueOf(1), itr.next());
    assertEquals(Integer.valueOf(2), itr.next());
    assertEquals(Integer.valueOf(3), itr.next());
    try {
      itr.hasNext();
      fail("Expected an exception");
    } catch (Exception e) {
      assertSame(ex, e);
    }
  }
}
