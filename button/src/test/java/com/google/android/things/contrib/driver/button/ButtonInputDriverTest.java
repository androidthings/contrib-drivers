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
package com.google.android.things.contrib.driver.button;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.Mockito.times;

public class ButtonInputDriverTest {

    @Mock
    Button mButton;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void close() throws IOException {
        ButtonInputDriver driver = new ButtonInputDriver(mButton, 0);
        driver.close();
        Mockito.verify(mButton).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        ButtonInputDriver driver = new ButtonInputDriver(mButton, 0);
        driver.close();
        driver.close(); // should not throw
        Mockito.verify(mButton, times(1)).close();
    }

    @Test
    public void register_throwsIfClosed() throws IOException {
        ButtonInputDriver driver = new ButtonInputDriver(mButton, 0);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.register();
    }

}
