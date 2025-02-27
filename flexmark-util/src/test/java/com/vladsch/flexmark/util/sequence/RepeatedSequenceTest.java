/*
 * Copyright (c) 2016-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
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

package com.vladsch.flexmark.util.sequence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RepeatedSequenceTest {
  @Test
  public void test_basic() {
    String orig = "abcdef";

    CharSequence test = RepeatedSequence.repeatOf(orig, 2);
    String result = orig + orig;
    assertEquals(result, test.toString());
    assertEquals(result.length(), test.length());
    assertEquals(result.hashCode(), test.hashCode());
    test_subSequence(result, test);
    test_charAt(result, test);
  }

  @Test
  public void test_partial() {
    String orig = "abcdef";

    CharSequence test = RepeatedSequence.repeatOf(orig, 3, orig.length() + 3);
    String result = orig.substring(3) + orig.substring(0, 3);
    assertEquals(result, test.toString());
    assertEquals(result.length(), test.length());
    assertEquals(result.hashCode(), test.hashCode());
    test_subSequence(result, test);
    test_charAt(result, test);

    test = RepeatedSequence.repeatOf(orig, 3, orig.length() + 5);
    result = orig.substring(3) + orig.substring(0, 5);
    assertEquals(result, test.toString());
    assertEquals(result.length(), test.length());
    assertEquals(result.hashCode(), test.hashCode());
    test_subSequence(result, test);
    test_charAt(result, test);
  }

  private static void test_charAt(String result, CharSequence chars) {
    int iMax = result.length();
    for (int i = 0; i < iMax; i++) {
      assertEquals(String.valueOf(result.charAt(i)), String.valueOf(chars.charAt(i)));
    }
  }

  private static void test_subSequence(String result, CharSequence chars) {
    int iMax = result.length();
    for (int i = 0; i < iMax; i++) {
      for (int j = iMax - i - 1; j >= 0 && j >= i; j--) {
        assertEquals(
            "subSequence(" + i + "," + j + ")",
            result.substring(i, j),
            chars.subSequence(i, j).toString());
        assertEquals(
            "subSequence(" + i + "," + j + ").hashCode()",
            result.subSequence(i, j).hashCode(),
            chars.subSequence(i, j).hashCode());
        assertEquals(chars.subSequence(i, j), result.subSequence(i, j));
      }
    }
  }
}
