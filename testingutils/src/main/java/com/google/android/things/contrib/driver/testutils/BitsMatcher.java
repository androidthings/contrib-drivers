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
public class BitsMatcher<T extends Number> extends TypeSafeMatcher<T> {

    private final int mBits;
    private final boolean mNotSet;

    private BitsMatcher(byte bits, boolean notSet) {
        mBits = bits & 0xFF;
        mNotSet = notSet;
    }

    private BitsMatcher(short bits, boolean notSet) {
        mBits = bits & 0xFFFF;
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
    public static BitsMatcher<Byte> hasBitsSet(byte mask) {
        return new BitsMatcher<>(mask, false);
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
    public static BitsMatcher<Byte> hasBitsNotSet(byte mask) {
        return new BitsMatcher<>(mask, true);
    }

    public static BitsMatcher<Short> hasBitsSet(short mask) {
        return new BitsMatcher<>(mask, false);
    }

    public static BitsMatcher<Short> hasBitsNotSet(short mask) {
        return new BitsMatcher<>(mask, true);
    }

    @Override
    public void describeTo(Description description) {
        int bits = mNotSet ? 0 : 1;
        description.appendText(String.format("Value must be a byte with matching %d bits: %s",
                bits, Integer.toBinaryString(mBits))); // suppress sign extension
    }

    @Override
    protected void describeMismatchSafely(T item, Description mismatchDescription) {
        int bits = mNotSet ? 0 : 1;
        mismatchDescription.appendText(String.format("Value does not have matching %d bits: %s",
                bits, Integer.toBinaryString(mBits))); // suppress sign extension
    }

    @Override
    protected boolean matchesSafely(T item) {
        int matchItem = item.intValue();
        matchItem &= (item instanceof Byte) ? 0xFF : 0xFFFF;
        if (mNotSet) {
            int minusOne = (item instanceof Byte) ? 0xFF : 0xFFFF;
            return mBits == minusOne ? matchItem == minusOne : (matchItem | mBits) == mBits;
        }
        return mBits == 0 ? matchItem == 0 : (matchItem & mBits) == mBits;
    }
}
