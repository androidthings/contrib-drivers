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

import static com.google.android.things.contrib.driver.testutils.BitsMatcher.hasBitsNotSet;
import static com.google.android.things.contrib.driver.testutils.BitsMatcher.hasBitsSet;

import static junit.framework.Assert.assertEquals;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.byteThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Handler;

import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx.AlertCallback;
import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx.Configuration;
import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx.OnCapTouchListener;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest({Cap1xxx.class, Gpio.class})
public class Cap1xxxTest {

    @Mock
    I2cDevice mI2c;

    @Mock
    Gpio mGpio;

    @Mock
    AlertCallback mGpioCallback;

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    // Configuration used for all tests
    private static final Configuration CONFIGURATION = Configuration.CAP1188;

    private Cap1xxx mDriver;

    @Before
    public void setup() throws Exception {
        mGpio = PowerMockito.mock(Gpio.class);
        mGpioCallback = PowerMockito.mock(AlertCallback.class);
        PowerMockito.doNothing().when(mGpio).registerGpioCallback(any(GpioCallback.class));
        PowerMockito.whenNew(AlertCallback.class).withNoArguments().thenReturn(mGpioCallback);

        mDriver = new Cap1xxx(mI2c, mGpio, CONFIGURATION);
        // clear invocations from driver init
        Mockito.reset(mGpio);
        Mockito.reset(mI2c);
    }

    @Test
    public void driverInit() throws IOException {
        // re-initialize since we want to verify invocations from driver init
        mDriver = new Cap1xxx(mI2c, mGpio, CONFIGURATION);
        verify(mGpio).setDirection(Gpio.DIRECTION_IN);
        verify(mGpio).setEdgeTriggerType(Gpio.EDGE_FALLING);
        verify(mGpio).registerGpioCallback(any(Handler.class), any(GpioCallback.class));

        // setInputsEnabled
        verify(mI2c).writeRegByte(0x21, (byte) 0xFF);
        // setInterruptsEnabled
        verify(mI2c).writeRegByte(0x27, (byte) 0xFF);
        // setMultitouchInputMax
        verify(mI2c).writeRegByte(eq(0x2A),
                byteThat(hasBitsSet((byte) 0b10001100)));
        // setRepeatRate
        verify(mI2c).writeRegByte(0x28, (byte) 0xFF);
        verify(mI2c).writeRegByte(eq(0x22),
                byteThat(hasBitsSet((byte) (0x0F & Cap1xxx.REPEAT_NORMAL))));
        verify(mI2c).writeRegByte(eq(0x23),
                byteThat(hasBitsSet((byte) (0x0F & Cap1xxx.REPEAT_NORMAL))));
        // setSensitivity
        Mockito.verify(mI2c).writeRegByte(eq(0x1F),
                byteThat(hasBitsSet((byte) (0x70 & Cap1xxx.SENSITIVITY_NORMAL))));
        // setLedFade
        Mockito.verify(mI2c).writeRegByte(0x94, (byte) Cap1xxx.LED_FADE_INSTANT);
        // setLedBrightness
        Mockito.verify(mI2c).writeRegByte(0x93, (byte) 0xF0);
        // setLedInputLinkEnabled
        Mockito.verify(mI2c).writeRegByte(0x72, (byte) 0x00);
        // Turn off LEDs.
        Mockito.verify(mI2c).writeRegByte(0x74, (byte) 0x00);

        assertEquals(CONFIGURATION.channelCount, mDriver.getInputChannelCount());
        assertEquals(CONFIGURATION.maxTouch, mDriver.getMaximumTouchPoints());
    }

    @Test
    public void close() throws IOException {
        mDriver.close();
        verify(mI2c).close();
        verify(mGpio).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        mDriver.close();
        mDriver.close();
        verify(mI2c, times(1)).close();
        verify(mGpio, times(1)).close();
    }

    @Test
    public void clearInterruptFlag() throws IOException {
        mDriver.clearInterruptFlag();
        verify(mI2c).readRegByte(0x00);
        verify(mI2c).writeRegByte(eq(0x00), byteThat(hasBitsNotSet((byte) 0b11111110)));
    }

    @Test
    public void readInputChannel_throwsIfTooSmall() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.readInputChannel(-1);
    }

    @Test
    public void readInputChannel_throwsIfTooSLarge() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.readInputChannel(CONFIGURATION.channelCount + 1);
    }

    @Test
    public void readLedState_throwsIfTooSmall() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.readLedState(-1);
    }

    @Test
    public void readLedState_throwsIfTooSLarge() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.readLedState(CONFIGURATION.ledCount + 1);
    }

    @Test
    public void setLedState_throwsIfTooSmall() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.setLedState(-1, true);
    }

    @Test
    public void setLedState_throwsIfTooSLarge() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.setLedState(CONFIGURATION.ledCount + 1, true);
    }

    @Test
    public void readInterruptFlag() throws IOException {
        mDriver = new Cap1xxx(mI2c, mGpio, CONFIGURATION);
        mDriver.readInterruptFlag();
        verify(mI2c).readRegByte(0x00);
    }

    @Test
    public void readInterruptFlag_clearBit() throws IOException {
        mDriver = new Cap1xxx(mI2c, mGpio, CONFIGURATION);

        Mockito.when(mI2c.readRegByte(0x00)).thenReturn((byte) 1);
        // Should not clear
        mDriver.readInterruptFlag(false);
        verify(mI2c).readRegByte(0x00);
        verify(mI2c, never()).writeRegByte(eq(0x00), anyByte());

        Mockito.reset(mI2c);

        Mockito.when(mI2c.readRegByte(0x00)).thenReturn((byte) 1);
        // Should clear
        mDriver.readInterruptFlag(true);
        verify(mI2c).readRegByte(0x00);
        verify(mI2c).writeRegByte(eq(0x00), byteThat(hasBitsNotSet((byte) 0b11111110)));
    }

    @Test
    public void setInputsEnabled() throws IOException {
        mDriver.setInputsEnabled(true);
        verify(mI2c).writeRegByte(0x21, (byte) 0xFF);
        mDriver.setInputsEnabled(false);
        verify(mI2c).writeRegByte(0x21, (byte) 0);
    }

    @Test
    public void setInterruptsEnabled() throws IOException {
        mDriver.setInterruptsEnabled(true);
        verify(mI2c).writeRegByte(0x27, (byte) 0xFF);
        mDriver.setInterruptsEnabled(false);
        verify(mI2c).writeRegByte(0x27, (byte) 0);
    }

    @Test
    public void setMultitouchInputMax() throws IOException {
        mDriver.setMultitouchInputMax(CONFIGURATION.maxTouch);
        final int mask = (CONFIGURATION.maxTouch - 1) << 2;
        verify(mI2c).writeRegByte(eq(0x2A), byteThat(hasBitsSet((byte) mask)));

        Mockito.reset(mI2c);

        mDriver.setMultitouchInputMax(1);
        // mask for this amount of inputs is zero, so invert the check
        verify(mI2c).writeRegByte(eq(0x2A), byteThat(hasBitsNotSet((byte) ~0x0C)));
    }

    @Test
    public void setMultitouchInputMax_throwsIfTooSmall() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.setMultitouchInputMax(0);
    }

    @Test
    public void setMultitouchInputMax_throwsIfTooLarge() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.setMultitouchInputMax(CONFIGURATION.maxTouch + 1);
    }

    @Test
    public void setRepeatEnabled() throws IOException {
        mDriver.setRepeatEnabled(true);
        verify(mI2c).writeRegByte(0x28, (byte) 0xFF);
        mDriver.setRepeatEnabled(false);
        verify(mI2c).writeRegByte(0x28, (byte) 0);
    }

    @Test
    public void setLedInputLinkEnabled() throws IOException {
        mDriver.setLedInputLinkEnabled(true);
        verify(mI2c).writeRegByte(0x72, (byte) 0xFF);
        mDriver.setLedInputLinkEnabled(false);
        verify(mI2c).writeRegByte(0x72, (byte) 0);
    }

    @Test
    public void setRepeatRate() throws IOException {
        mDriver.setRepeatRate(Cap1xxx.REPEAT_DISABLE);
        verify(mI2c).writeRegByte(0x28, (byte) 0);

        Mockito.reset(mI2c);

        mDriver.setRepeatRate(Cap1xxx.REPEAT_SLOW);
        verify(mI2c).writeRegByte(0x28, (byte) 0xFF);
        verify(mI2c).writeRegByte(eq(0x22),
                byteThat(hasBitsSet((byte) (0x0F & Cap1xxx.REPEAT_SLOW))));
        verify(mI2c).writeRegByte(eq(0x23),
                byteThat(hasBitsSet((byte) (0x0F & Cap1xxx.REPEAT_SLOW))));

        Mockito.reset(mI2c);

        mDriver.setRepeatRate(Cap1xxx.REPEAT_FAST);
        verify(mI2c).writeRegByte(0x28, (byte) 0xFF);
        verify(mI2c).writeRegByte(eq(0x22),
                byteThat(hasBitsSet((byte) (0x0F & Cap1xxx.REPEAT_FAST))));
        verify(mI2c).writeRegByte(eq(0x23),
                byteThat(hasBitsSet((byte) (0x0F & Cap1xxx.REPEAT_FAST))));
    }

    @Test
    public void setSensitivity() throws IOException {
        mDriver.setSensitivity(Cap1xxx.SENSITIVITY_LOW);
        verify(mI2c).writeRegByte(eq(0x1F),
                byteThat(hasBitsSet((byte) (0x70 & Cap1xxx.SENSITIVITY_LOW))));

        Mockito.reset(mI2c);

        mDriver.setSensitivity(Cap1xxx.SENSITIVITY_HIGH);
        // mask for this sensitivity is zero, so invert the check
        verify(mI2c).writeRegByte(eq(0x1F), byteThat(hasBitsNotSet((byte) ~0x70)));
    }

    @Test
    public void setLedFade() throws IOException {
        mDriver.setLedFade(Cap1xxx.LED_FADE_FAST);
        verify(mI2c).writeRegByte(0x94, (byte) Cap1xxx.LED_FADE_FAST);

        Mockito.reset(mI2c);

        mDriver.setLedFade(Cap1xxx.LED_FADE_SLOW);
        verify(mI2c).writeRegByte(0x94, (byte) Cap1xxx.LED_FADE_SLOW);
    }

    @Test
    public void setLedBrightness() throws IOException {
        mDriver.setLedBrightness(0);
        verify(mI2c).writeRegByte(0x93, (byte) 0x00);

        Mockito.reset(mI2c);

        mDriver.setLedBrightness(1);
        verify(mI2c).writeRegByte(0x93, (byte) 0xF0);
    }

    @Test
    public void setLedBrightness_throwsIfTooSmall() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.setLedBrightness(-0.1f);
    }

    @Test
    public void setLedBrightness_throwsIfTooLarge() throws IOException {
        mExpectedException.expect(IllegalArgumentException.class);
        mDriver.setLedBrightness(1.1f);
    }

    @Test
    public void setOnCapTouchListener() throws IOException {
        // Setup expected values
        final boolean[] expectedStatus = new boolean[CONFIGURATION.channelCount];
        int activeChannels = 0;
        // alternating true/false values
        for (int i = 0; i < expectedStatus.length; i += 2) {
            expectedStatus[i] = true;
            activeChannels |= (1 << i);
        }
        // Stub I2cDevice
        Mockito.when(mI2c.readRegByte(0x03)).thenReturn((byte) activeChannels);
        Mockito.when(mI2c.readRegByte(0x00)).thenReturn((byte) 0b00000001); // interrupt
        // Make input deltas exceed threshold readings
        Mockito.doAnswer(new ArrayFillingAnswer((byte) 10)) // input deltas
                .when(mI2c)
                .readRegBuffer(eq(0x10), any(byte[].class), eq(CONFIGURATION.channelCount));
        Mockito.doAnswer(new ArrayFillingAnswer((byte) 5)) // input thresholds
                .when(mI2c)
                .readRegBuffer(eq(0x30), any(byte[].class), eq(CONFIGURATION.channelCount));

        // Add a listener
        OnCapTouchListener mockListener = Mockito.mock(OnCapTouchListener.class);
        mDriver.setOnCapTouchListener(mockListener);
        // Trigger a read
        mDriver.handleInterrupt();
        // Verify the listener was called
        Mockito.verify(mockListener, times(1)).onCapTouchEvent(eq(mDriver), aryEq(expectedStatus));

        // Remove the listener
        mDriver.setOnCapTouchListener(null);
        // Trigger a read
        mDriver.handleInterrupt();
        // Verify the listener was not called
        Mockito.verifyNoMoreInteractions(mockListener);
    }
}
