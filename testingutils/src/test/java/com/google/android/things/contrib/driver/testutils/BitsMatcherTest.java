/*
 * Copyright 2017 Google Inc.
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
package com.google.android.things.contrib.driver.testutils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BitsMatcherTest {

    @Test
    public void bitsSet() {
        BitsMatcher<Byte> byteMatcher = BitsMatcher.hasBitsSet((byte) 0b00001010); // 10

        // bits set
        assertTrue(byteMatcher.matches((byte) 10));
        assertTrue(byteMatcher.matches((byte) 0b00001011));
        assertTrue(byteMatcher.matches((byte) 0b00011010));

        // bits not set
        assertFalse(byteMatcher.matches((byte) 0));
        assertFalse(byteMatcher.matches((byte) 0b00011001));
        assertFalse(byteMatcher.matches((byte) 0b01000000));

        BitsMatcher<Short> shortMatcher =
                BitsMatcher.hasBitsSet((short) 0b1000000000001010); // -32758

        // bits set
        assertTrue(shortMatcher.matches((short) -32758));
        assertTrue(shortMatcher.matches((short) 0b1000000000001011));
        assertTrue(shortMatcher.matches((short) 0b1010101010101010));
        assertTrue(shortMatcher.matches((short) 0b1000111100001010));

        // bits not set
        assertFalse(shortMatcher.matches((short) 0));
        assertFalse(shortMatcher.matches((short) 0b0000000000001010));
        assertFalse(shortMatcher.matches((short) 0b1000000111100010));
        assertFalse(shortMatcher.matches((short) 0b1000000111100001));
    }

    @Test
    public void bitsSet_allUnset() {
        BitsMatcher<Byte> byteMatcher = BitsMatcher.hasBitsSet((byte) 0); // all 0s
        assertTrue(byteMatcher.matches((byte) 0));
        assertFalse(byteMatcher.matches((byte) 1));

        BitsMatcher<Short> shortMatcher = BitsMatcher.hasBitsSet((short) 0); // all 0s
        assertTrue(shortMatcher.matches((short) 0));
        assertFalse(shortMatcher.matches((short) 1));
    }

    @Test
    public void bitsNotSet() {
        BitsMatcher<Byte> byteMatcher = BitsMatcher.hasBitsNotSet((byte) 0b11111100); // -4

        // bits not set
        assertTrue(byteMatcher.matches((byte) -4));
        assertTrue(byteMatcher.matches((byte) 0b00001000));
        assertTrue(byteMatcher.matches((byte) 0b11011000));
        assertTrue(byteMatcher.matches((byte) 0));

        // bits set
        assertFalse(byteMatcher.matches((byte) 1));
        assertFalse(byteMatcher.matches((byte) 0b01000010));
        assertFalse(byteMatcher.matches((byte) 0b11000011));

        BitsMatcher<Short> shortMatcher =
                BitsMatcher.hasBitsNotSet((short) 0b1010101000001010); // -22006

        // bits not set
        assertTrue(shortMatcher.matches((short) -22006));
        assertTrue(shortMatcher.matches((short) 0b1010101000001010));
        assertTrue(shortMatcher.matches((short) 0b1000101000000000));
        assertTrue(shortMatcher.matches((short) 0));

        // bits set
        assertFalse(shortMatcher.matches((short) 1));
        assertFalse(shortMatcher.matches((short) 0b1010101111111010));
        assertFalse(shortMatcher.matches((short) 0b1110111000001010));
    }

    @Test
    public void bitsNotSet_allSet() {
        BitsMatcher<Byte> byteMatcher = BitsMatcher.hasBitsNotSet((byte) -1); // all 1s
        assertTrue(byteMatcher.matches((byte) -1));
        assertFalse(byteMatcher.matches(Byte.MAX_VALUE)); // all 1s except MSB

        BitsMatcher<Short> shortMatcher = BitsMatcher.hasBitsNotSet((short) -1); // all 1s
        assertTrue(shortMatcher.matches((short) -1));
        assertFalse(shortMatcher.matches(Short.MAX_VALUE)); // all 1s except MSB
    }
}
