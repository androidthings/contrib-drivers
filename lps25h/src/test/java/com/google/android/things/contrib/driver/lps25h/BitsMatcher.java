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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher that checks if necessary bits are set in a byte argument.
 */
public class BitsMatcher extends TypeSafeMatcher<Byte> {

    private final byte mBits;

    public BitsMatcher(byte bits) {
        mBits = bits;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Value must be a byte with bits set: ")
                .appendText(Integer.toBinaryString(mBits));
    }

    @Override
    protected void describeMismatchSafely(Byte item, Description mismatchDescription) {
        mismatchDescription.appendText("Value should have bits set: ")
                .appendText(Integer.toBinaryString(mBits));
    }

    @Override
    protected boolean matchesSafely(Byte item) {
        return mBits == 0 ? item == 0 : (item & mBits) == mBits;
    }
}

