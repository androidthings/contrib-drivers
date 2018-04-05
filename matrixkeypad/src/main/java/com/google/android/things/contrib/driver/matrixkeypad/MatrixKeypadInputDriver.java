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

import android.os.Handler;
import android.view.InputDevice;
import android.view.KeyEvent;
import com.google.android.things.contrib.driver.matrixkeypad.MatrixKeypad.OnKeyEventListener;
import com.google.android.things.pio.Gpio;
import com.google.android.things.userdriver.input.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.input.InputDriverEvent;
import java.io.IOException;

/**
 * Driver class for a {@link MatrixKeypad} which registers an {@link InputDriver} to send key events
 * to the system.
 */
public class MatrixKeypadInputDriver implements AutoCloseable {
    private static final String DEVICE_NAME = "Matrix Keypad";

    private MatrixKeypad mMatrixKeypad;
    private InputDriver mInputDriver;
    private int[] mKeyCodes;
    private InputDriverEvent mInputEvent = new InputDriverEvent();
    private OnKeyEventListener mMatrixKeyCallback = new OnKeyEventListener() {
        @Override
        public void onKeyEvent(MatrixKey matrixKey) {
            mInputEvent.clear();
            mInputEvent.setKeyPressed(matrixKey.getKeyCode(), matrixKey.isPressed());
            mInputDriver.emit(mInputEvent);
        }
    };

    /**
     * Creates a new driver for your matrix keypad.
     *
     * @param rowPins The string names for each pin related to rows, from row 1 - row M.
     * @param colPins The string names for each pin related to cols, from col 1 - col N.
     * @param keyCodes The integer keycodes for each key, from top-left to bottom-right.
     * @throws IOException If there's a problem connecting to any pin.
     */
    public MatrixKeypadInputDriver(String[] rowPins, String[] colPins, int[] keyCodes)
            throws IOException {
        mMatrixKeypad = new MatrixKeypad(rowPins, colPins, keyCodes);
        mMatrixKeypad.setKeyCallback(mMatrixKeyCallback);
        mKeyCodes = keyCodes;
    }

    /* package */ MatrixKeypadInputDriver(Gpio[] rowGpio, Gpio[] colGpio, int[] keyCodes,
            Handler handler) throws IOException {
        mMatrixKeypad = new MatrixKeypad(rowGpio, colGpio, keyCodes, handler);
        mMatrixKeypad.setKeyCallback(mMatrixKeyCallback);
        mKeyCodes = keyCodes;
    }

    /* package */ MatrixKeypadInputDriver(Gpio[] rowGpio, Gpio[] colGpio, int[] keyCodes)
            throws IOException {
        mMatrixKeypad = new MatrixKeypad(rowGpio, colGpio, keyCodes);
        mMatrixKeypad.setKeyCallback(mMatrixKeyCallback);
        mKeyCodes = keyCodes;
    }

    public void register() {
        if (mInputDriver == null) {
            mInputDriver = new InputDriver.Builder()
                    .setName(DEVICE_NAME)
                    .setSupportedKeys(mKeyCodes)
                    .build();
            UserDriverManager.getInstance().registerInputDriver(mInputDriver);
        }
    }

    /**
     * Emits a key down event through the input driver
     *
     * @param keycode keycode to send
     */
    /* package */ void keyDown(int keycode) {
        mMatrixKeypad.keyDown(keycode);
    }

    /**
     * Emits a key up event through the input driver
     *
     * @param keycode keycode to send
     */
    /* package */ void keyUp(int keycode) {
        mMatrixKeypad.keyUp(keycode);
    }

    public void unregister() {
        if (mInputDriver != null) {
            UserDriverManager.getInstance().unregisterInputDriver(mInputDriver);
            mInputDriver = null;
        }
    }

    @Override
    public void close() throws IOException {
        unregister();

        if (mMatrixKeypad != null) {
            try {
                mMatrixKeypad.close();
            } finally {
                mMatrixKeypad = null;
            }
        }
    }
}
