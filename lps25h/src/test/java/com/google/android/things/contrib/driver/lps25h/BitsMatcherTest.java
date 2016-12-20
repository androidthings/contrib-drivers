/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.things.contrib.driver.lps25h;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BitsMatcherTest {

    @Test
    public void matches_positive() {
        BitsMatcher matcher = new BitsMatcher((byte) 10); // 00001010

        // bits set
        assertTrue(matcher.matches((byte) 10));
        assertTrue(matcher.matches((byte) 0b00001011));
        assertTrue(matcher.matches((byte) 0b00011010));

        // bits not set
        assertFalse(matcher.matches((byte) 0));
        assertFalse(matcher.matches((byte) 0b00011001));
        assertFalse(matcher.matches((byte) 0b01000000));
    }

    @Test
    public void matches_negative() {
        BitsMatcher matcher = new BitsMatcher((byte) -31); // 11100001

        // bits set
        assertTrue(matcher.matches((byte) -31));
        assertTrue(matcher.matches((byte) 0b11111011));
        assertTrue(matcher.matches((byte) 0b11101001));

        // bits not set
        assertFalse(matcher.matches((byte) 0));
        assertFalse(matcher.matches((byte) 0b11100000));
        assertFalse(matcher.matches((byte) 0b11000001));
    }

    @Test
    public void matches_zero() {
        BitsMatcher matcher = new BitsMatcher((byte) 0);
        assertTrue(matcher.matches((byte) 0));
        assertFalse(matcher.matches((byte) 1));
    }
}
