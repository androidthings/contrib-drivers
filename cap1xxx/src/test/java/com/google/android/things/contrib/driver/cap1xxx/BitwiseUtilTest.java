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
package com.google.android.things.contrib.driver.cap1xxx;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class BitwiseUtilTest {

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void setBit() {
        assertEquals((byte) 0b01, BitwiseUtil.setBit((byte) 0, 0));
        assertEquals((byte) 0b10, BitwiseUtil.setBit((byte) 0, 1));
        // bitIndex outside of byte range
        assertEquals((byte) 0, BitwiseUtil.setBit((byte) 0, 8));
    }

    @Test
    public void clearBit() {
        assertEquals((byte) 0, BitwiseUtil.clearBit((byte) 0b01, 0));
        assertEquals((byte) 0, BitwiseUtil.clearBit((byte) 0b10, 1));
        // bitIndex outside of byte range
        assertEquals((byte) 0xFF, BitwiseUtil.clearBit((byte) 0xFF, 8));
    }

    @Test
    public void isBitSet() {
        assertTrue(BitwiseUtil.isBitSet((byte) 0b01, 0));
        assertTrue(BitwiseUtil.isBitSet((byte) 0b10, 1));

        assertFalse(BitwiseUtil.isBitSet((byte) 0b11111110, 0));
        assertFalse(BitwiseUtil.isBitSet((byte) 0b11111101, 1));

        // bitIndex outside of byte range
        assertFalse(BitwiseUtil.isBitSet((byte) 0xFF, 8));
    }

    @Test
    public void applyBitRange() {
        assertEquals((byte) 0b10101010,
                BitwiseUtil.applyBitRange((byte) 0b11111111, 0, 0b01010101));

        assertEquals((byte) 0b11001100,
                BitwiseUtil.applyBitRange((byte) 0b11110000, 0b00001111, 0b00111100));

        // empty mask, no change
        assertEquals((byte) 0b00001111,
                BitwiseUtil.applyBitRange((byte) 0b00001111, 0b11110000, 0));

        // mask outside of byte range, no change
        assertEquals((byte) 1, BitwiseUtil.applyBitRange((byte) 1, -1, 0xFF00));
    }
}
