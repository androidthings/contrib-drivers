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

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;

public class ArrayFillingAnswer implements Answer<Void> {
    private byte mValue;

    ArrayFillingAnswer(byte value) {
        mValue = value;
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
        byte[] buffer = invocation.getArgumentAt(1, byte[].class);
        Arrays.fill(buffer, mValue);
        return null;
    }
}
