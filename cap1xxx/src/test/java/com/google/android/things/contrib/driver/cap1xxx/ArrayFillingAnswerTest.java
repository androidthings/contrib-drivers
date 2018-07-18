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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;

import java.lang.reflect.Method;

public class ArrayFillingAnswerTest {

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void fillsArrayAndReturnsNull() throws Throwable {
        final byte value = 42;
        ArrayFillingAnswer answer = new ArrayFillingAnswer(value);
        final byte[] toBeFilled = new byte[10];

        InvocationOnMock invocation = new InvocationOnMock() {
            @Override
            public Object getMock() {
                return null;
            }

            @Override
            public Method getMethod() {
                return null;
            }

            @Override
            public Object[] getArguments() {
                return new Object[] {
                        null,
                        toBeFilled,
                        null
                };
            }

            @Override
            public <T> T getArgumentAt(int index, Class<T> clazz) {
                return (T) getArguments()[index];
            }

            @Override
            public Object callRealMethod() throws Throwable {
                return null;
            }
        };

        assertNull(answer.answer(invocation));
        for (byte b : toBeFilled) {
            assertEquals(value, b);
        }
    }
}
