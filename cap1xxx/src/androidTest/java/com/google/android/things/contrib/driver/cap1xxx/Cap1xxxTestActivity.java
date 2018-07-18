package com.google.android.things.contrib.driver.cap12xx;/*
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx.Configuration;
import com.google.android.things.pio.I2cDevice;

import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Cap12xxTestActivity extends Activity {

    private static final String TAG = "Cap1xxxTestActivity";

    static final Configuration CONFIGURATION = Configuration.CAP1208;
    static final int[] KEYCODES = {
            KeyEvent.KEYCODE_0,
            KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7,
    };

    private static final ArrayFillingAnswer sInputThresholdAnswer =
            new ArrayFillingAnswer((byte) 5);
    private static final ArrayFillingAnswer sInputDeltaHighAnswer =
            new ArrayFillingAnswer((byte) 10);
    private static final ArrayFillingAnswer sInputDeltaLowAnswer =
            new ArrayFillingAnswer((byte) 0);

    private I2cDevice mI2c;
    private Cap1xxx mCap1xxx;
    private Cap1xxxInputDriver mInputDriver;

    private BlockingQueue<KeyEvent> mKeyDownEvents = new LinkedBlockingQueue<>();
    private BlockingQueue<KeyEvent> mKeyUpEvents = new LinkedBlockingQueue<>();

    private static I2cDevice initMockI2cDevice() throws IOException {
        I2cDevice i2c = Mockito.mock(I2cDevice.class);
        // set input thresholds
        Mockito.doAnswer(sInputThresholdAnswer)
                .when(i2c)
                .readRegBuffer(eq(0x30), any(byte[].class), eq(CONFIGURATION.channelCount));
        return i2c;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mI2c = initMockI2cDevice();
            mCap1xxx = new Cap1xxx(mI2c, null, Configuration.CAP1208);
            mInputDriver = new Cap12xxInputDriver(mCap1xxx, KEYCODES);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing input driver", e);
        }

        mInputDriver.register();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInputDriver != null) {
            mInputDriver.unregister();
            try {
                mInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input driver", e);
            } finally {
                mInputDriver = null;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mKeyDownEvents.add(event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mKeyUpEvents.add(event);
        return super.onKeyUp(keyCode, event);
    }

    @VisibleForTesting
    void testKeyEvents() throws IOException {
        for (int i = 0; i < KEYCODES.length; i++) {
            stubInterruptsForChannel(i);

            KeyEvent downEvent = null;
            KeyEvent upEvent = null;

            try {
                downEvent = mKeyDownEvents.poll(1, TimeUnit.SECONDS);
                upEvent = mKeyUpEvents.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted!", e);
            }
            verifyKeyEvents(KEYCODES[i], downEvent, upEvent);
        }
    }

    private void stubInterruptsForChannel(int channel) throws IOException {
        Mockito.doAnswer(sInputDeltaHighAnswer) // deltas for key down
                .doAnswer(sInputDeltaLowAnswer) // deltas for key up
                .when(mI2c)
                .readRegBuffer(eq(0x10), any(byte[].class), eq(CONFIGURATION.channelCount));

        final int activeChannel = 1 << channel;
        Mockito.when(mI2c.readRegByte(0x03))
                .thenReturn((byte) activeChannel) // specified channel is active
                .thenReturn((byte) activeChannel)
                .thenReturn((byte) 0); // no channels active

        Mockito.when(mI2c.readRegByte(0x00))
                .thenReturn((byte) 0b00000001) // interrupt, key down
                .thenReturn((byte) 0b00000001) // interrupt, key up
                .thenReturn((byte) 0); // no more interrupts
    }

    private void verifyKeyEvents(int keycode, KeyEvent downEvent, KeyEvent upEvent) {
        assertNotNull(downEvent);
        assertNotNull(upEvent);
        assertEquals(keycode, downEvent.getKeyCode());
        assertEquals(keycode, upEvent.getKeyCode());
    }
}
