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

package com.google.android.things.contrib.driver.apa102;

import android.graphics.Color;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.Matchers.anyInt;

public class ColorMock {
    public static void mockStatic() {
        PowerMockito.mockStatic(Color.class);
        Mockito.when(Color.red(anyInt())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int c = invocation.getArgumentAt(0, Integer.class);
                return (c >> 16) & 0xff;
            }
        });
        Mockito.when(Color.green(anyInt())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int c = invocation.getArgumentAt(0, Integer.class);
                return (c >> 8) & 0xff;
            }
        });
        Mockito.when(Color.blue(anyInt())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                int c = invocation.getArgumentAt(0, Integer.class);
                return c & 0xff;
            }
        });

        Mockito.when(Color.argb(anyInt(), anyInt(), anyInt(), anyInt())).thenAnswer(new Answer() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                int a = invocation.getArgumentAt(0, Integer.class);
                int r = invocation.getArgumentAt(1, Integer.class);
                int g = invocation.getArgumentAt(2, Integer.class);
                int b = invocation.getArgumentAt(3, Integer.class);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        });
    }
}
