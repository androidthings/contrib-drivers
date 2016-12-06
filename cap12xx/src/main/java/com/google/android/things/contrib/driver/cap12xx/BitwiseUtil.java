/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.cap12xx;

/**
 * Collection of helpers for bitwise operations
 */
class BitwiseUtil {

    /**
     * Set a bit in the provided value at the given index.
     * @return the updated value.
     */
    static byte setBit(byte value, int bitIndex) {
        return (byte)((value | (1 << bitIndex)) & 0xFF);
    }

    /**
     * Clear a bit in the provided value at the given index.
     * @return the updated value.
     */
    static byte clearBit(byte value, int bitIndex) {
        return (byte)((value & ~(1 << bitIndex)) & 0xFF);
    }

    /**
     * Return true if the provided bit index is set in the given value.
     */
    static boolean isBitSet(byte value, int bitIndex) {
        return (value & (1 << bitIndex)) != 0;
    }

    /**
     * Mask the bits of the provided value into base, according to
     * the provided bitmask and return the updated value.
     */
    static byte applyBitRange(byte base, int value, int bitmask) {
        base &= ~bitmask;
        base |= value & bitmask;

        return base;
    }
}
