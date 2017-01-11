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

import android.view.ViewConfiguration;

import com.google.android.things.contrib.driver.button.Button.InterruptCallback;
import com.google.android.things.contrib.driver.button.Button.LogicState;
import com.google.android.things.contrib.driver.button.Button.OnButtonEventListener;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ViewConfiguration.class, Button.class, Gpio.class})
public class ButtonTest {

    @Mock
    Gpio mGpio;

    @Mock
    InterruptCallback mGpioCallback;

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        ViewConfigurationMock.mockStatic();

        // Note: we need PowerMockito, so instantiate mocks here instead of using a MockitoRule
        mGpio = PowerMockito.mock(Gpio.class);
        mGpioCallback = PowerMockito.mock(InterruptCallback.class);
        PowerMockito.doNothing().when(mGpio).registerGpioCallback(any(GpioCallback.class));
        PowerMockito.whenNew(InterruptCallback.class).withNoArguments().thenReturn(mGpioCallback);
    }

    @Test
    public void close() throws IOException {
        Button button = new Button(mGpio, LogicState.PRESSED_WHEN_HIGH);
        button.close();
        Mockito.verify(mGpio).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Button button = new Button(mGpio, LogicState.PRESSED_WHEN_HIGH);
        button.close();
        button.close(); // should not throw
        Mockito.verify(mGpio, times(1)).close();
    }

    @Test
    public void setDebounceDelay() throws IOException {
        Button button = new Button(mGpio, LogicState.PRESSED_WHEN_HIGH);
        final long DELAY = 1000L;
        button.setDebounceDelay(DELAY);
        assertEquals(DELAY, button.getDebounceDelay());
    }

    @Test
    public void setDebounceDelay_throwsIfTooSmall() throws IOException {
        Button button = new Button(mGpio, LogicState.PRESSED_WHEN_HIGH);
        mExpectedException.expect(IllegalArgumentException.class);
        button.setDebounceDelay(-1);
    }

    @Test
    public void setButtonEventListener() throws IOException {
        Button button = new Button(mGpio, LogicState.PRESSED_WHEN_HIGH);
        final AtomicBoolean verifyPressed = new AtomicBoolean();

        button.setOnButtonEventListener(new OnButtonEventListener() {
            @Override
            public void onButtonEvent(Button b, boolean pressed) {
                verifyPressed.set(pressed);
            }
        });

        button.performButtonEvent(true);
        assertTrue(verifyPressed.get());
        button.performButtonEvent(false);
        assertFalse(verifyPressed.get());

        // remove listener
        button.setOnButtonEventListener(null);
        verifyPressed.set(false);
        button.performButtonEvent(true);
        assertFalse(verifyPressed.get()); // should not have been changed by the listener
    }

}
