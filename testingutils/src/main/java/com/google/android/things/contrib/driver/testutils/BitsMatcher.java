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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher that checks if necessary bits are set in a byte argument.
 */
public class BitsMatcher extends TypeSafeMatcher<Byte> {

    private final byte mBits;
    private final boolean mNotSet;

    private BitsMatcher(byte bits, boolean notSet) {
        mBits = bits;
        mNotSet = notSet;
    }

    /**
     * Create a BitsMatcher that checks arguments contain matching 1 bits when compared to the
     * provided mask, e.g.
     * <pre>
     *     BitsMatcher m = BitsMatcher.hasBitsSet((byte) 0b11111110);
     *     m.matches((byte) 0b11111111); // true
     *     m.matches((byte) 0b11111110); // true
     *     m.matches(OTHER_BYTE_VALUES); // false
     * </pre>
     *
     * @param mask mask containing 1s at each bit position that must be set. If the mask contains
     * all zeroes, only zero will match.
     */
    public static BitsMatcher hasBitsSet(byte mask) {
        return new BitsMatcher(mask, false);
    }

    /**
     * Create a BitsMatcher that checks arguments contain matching 0 bits when compared to the
     * provided mask, e.g.
     * <pre>
     *     BitsMatcher m = BitsMatcher.hasBitsNotSet((byte) 0b00000001);
     *     m.matches((byte) 0b00000000); // true
     *     m.matches((byte) 0b00000001); // true
     *     m.matches(OTHER_BYTE_VALUES); // false
     * </pre>
     *
     * @param mask mask containing 0s at each bit position that must be unset. If the mask contains
     * all ones, only -1 will match.
     */
    public static BitsMatcher hasBitsNotSet(byte mask) {
        return new BitsMatcher(mask, true);
    }

    @Override
    public void describeTo(Description description) {
        int bits = mNotSet ? 0 : 1;
        description.appendText(String.format("Value must be a byte with matching %d bits: %s",
                bits, Integer.toBinaryString(mBits & 0xFF))); // suppress sign extension
    }

    @Override
    protected void describeMismatchSafely(Byte item, Description mismatchDescription) {
        int bits = mNotSet ? 0 : 1;
        mismatchDescription.appendText(String.format("Value does not have matching %d bits: %s",
                bits, Integer.toBinaryString(mBits & 0xFF))); // suppress sign extension
    }

    @Override
    protected boolean matchesSafely(Byte item) {
        if (mNotSet) {
            return mBits == -1 ? item == -1 : (item | mBits) == mBits;
        }
        return mBits == 0 ? item == 0 : (item & mBits) == mBits;
    }
}
