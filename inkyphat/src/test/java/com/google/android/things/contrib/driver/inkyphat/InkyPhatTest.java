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
package com.google.android.things.contrib.driver.inkyphat;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.verify;

public class InkyPhatTest {

    @Mock
    SpiDevice mSpi;
    @Mock
    Gpio mBusyGpio;
    @Mock
    Gpio mResetGpio;
    @Mock
    Gpio mCommandGpio;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    private InkyPhatTriColourDisplay inkyPhat;

    @Before
    public void setUp() throws Exception {
        InkyPhat.Orientation orientation = InkyPhat.Orientation.LANDSCAPE;
        inkyPhat = new InkyPhatTriColourDisplay(
                mSpi,
                mBusyGpio,
                mResetGpio,
                mCommandGpio,
                new PixelBuffer(orientation),
                new ImageConverter(orientation),
                new ColorConverter()
        );
    }

    @Test
    public void modeIsZero() throws IOException {
        verify(mSpi).setMode(SpiDevice.MODE0);
    }

    @Test
    public void gpioIsActiveHigh() throws IOException {
        verify(mBusyGpio).setActiveType(Gpio.ACTIVE_HIGH);
        verify(mResetGpio).setActiveType(Gpio.ACTIVE_HIGH);
        verify(mCommandGpio).setActiveType(Gpio.ACTIVE_HIGH);
    }

    @Test
    public void commandIsOutputPin() throws IOException {
        verify(mCommandGpio).setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
    }

    @Test
    public void resetIsOutputPin() throws IOException {
        verify(mResetGpio).setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
    }

    @Test
    public void busyIsInputPin() throws IOException {
        verify(mBusyGpio).setDirection(Gpio.DIRECTION_IN);
    }

    @Test
    public void close_closesAllPeripherals() throws IOException {
        inkyPhat.close();
        Mockito.verify(mSpi).close();
        Mockito.verify(mBusyGpio).close();
        Mockito.verify(mResetGpio).close();
        Mockito.verify(mCommandGpio).close();
    }
}
