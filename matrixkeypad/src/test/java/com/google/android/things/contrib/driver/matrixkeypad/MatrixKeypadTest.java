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
package com.google.android.things.contrib.driver.matrixkeypad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.KeyEvent;
import com.google.android.things.contrib.driver.matrixkeypad.MatrixKeypad.OnKeyEventListener;
import com.google.android.things.pio.Gpio;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

public class MatrixKeypadTest {
    private static final Gpio[] mRowPins = new Gpio[] {PowerMockito.mock(Gpio.class)};
    private static final Gpio[] mColPins = new Gpio[] {PowerMockito.mock(Gpio.class)};
    private static final int[] mKeyCodes = new int[] {KeyEvent.KEYCODE_A};

    private MatrixKeypad mMatrixKeypad;

    /**
     * Tests that the callback will retrieve the expected values.
     * @throws IOException
     */
    @Test
    public void testCallbackKeyDown() throws IOException {
        final int[] index = {0};
        final int[] testKeys = new int[] {KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_NUMPAD_1,
                KeyEvent.KEYCODE_PERIOD};

        mMatrixKeypad = new MatrixKeypad(mRowPins, mColPins, mKeyCodes,
                PowerMockito.mock(Handler.class));
        mMatrixKeypad.setKeyCallback(new OnKeyEventListener() {
            @Override
            public void onKeyEvent(MatrixKey matrixKey) {
                assertEquals(testKeys[index[0]++], matrixKey.getKeyCode());
                assertTrue(matrixKey.isPressed());
            }
        });

        for (int key : testKeys) {
            mMatrixKeypad.keyDown(key);
        }
    }

    /**
     * Tests that the callback will retrieve the expected values.
     * @throws IOException
     */
    @Test
    public void testCallbackKeyUp() throws IOException {
        final int[] index = {0};
        final int[] testKeys = new int[] {KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_NUMPAD_1,
                KeyEvent.KEYCODE_PERIOD};

        mMatrixKeypad = new MatrixKeypad(mRowPins, mColPins, mKeyCodes,
                PowerMockito.mock(Handler.class));
        mMatrixKeypad.setKeyCallback(new OnKeyEventListener() {
            @Override
            public void onKeyEvent(MatrixKey matrixKey) {
                assertEquals(testKeys[index[0]++], matrixKey.getKeyCode());
                assertFalse(matrixKey.isPressed());
            }
        });

        for (int key : testKeys) {
            mMatrixKeypad.keyUp(key);
        }
    }

    @After
    public void closeDriver() throws IOException {
        mMatrixKeypad.close();
    }
}