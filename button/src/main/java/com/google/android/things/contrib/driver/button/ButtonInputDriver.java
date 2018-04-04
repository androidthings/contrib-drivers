/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.button;

import android.support.annotation.VisibleForTesting;
import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.input.InputDriver;
import com.google.android.things.userdriver.input.InputDriverEvent;
import java.io.IOException;

public class ButtonInputDriver implements AutoCloseable {
    private static final String DRIVER_NAME = "Button";

    private Button mDevice;
    private int mKeycode;
    private InputDriver mDriver;

    /**
     * Create a new framework input driver for a button connected on given GPIO pin.
     * The driver emits {@link android.view.KeyEvent} with the given keycode when registered.
     * @param pin GPIO pin where the button is connected.
     * @param logicLevel Logic level when the button is considered pressed.
     * @param keycode keycode to be emitted.
     * @throws IOException
     * @see #register
     * @return new input driver instance.
     */
    public ButtonInputDriver(String pin, Button.LogicState logicLevel, int keycode)
            throws IOException {
        mDevice = new Button(pin, logicLevel);
        mKeycode = keycode;
    }

    @VisibleForTesting
    /*package*/ ButtonInputDriver(Button button, int keycode) {
        mDevice = button;
        mKeycode = keycode;
    }

    /**
     * Set the time delay after an edge trigger that the button
     * must remain stable before generating an event. Debounce
     * is enabled by default for 100ms.
     *
     * Setting this value to zero disables debounce and triggers
     * events on all edges immediately.
     *
     * @param delay Delay, in milliseconds, or 0 to disable.
     */
    public void setDebounceDelay(long delay) {
        mDevice.setDebounceDelay(delay);
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregister();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Register the driver in the framework.
     */
    public void register() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }
        if (mDriver == null) {
            mDriver = build(mDevice, mKeycode);
            UserDriverManager.getInstance().registerInputDriver(mDriver);
        }
    }

    /**
     * Unregister the driver from the framework.
     */
    public void unregister() {
        if (mDriver != null) {
            UserDriverManager.getInstance().unregisterInputDriver(mDriver);
            mDriver = null;
        }
    }

    static InputDriver build(Button button, final int keyCode) {
        final InputDriver inputDriver = new InputDriver.Builder()
                .setName(DRIVER_NAME)
                .setSupportedKeys(new int[]{keyCode})
                .build();
        final InputDriverEvent inputEvent = new InputDriverEvent();
        button.setOnButtonEventListener(new Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(Button b, boolean pressed) {
                inputEvent.clear();
                inputEvent.setKeyPressed(keyCode, pressed);
                inputDriver.emit(inputEvent);
            }
        });
        return inputDriver;
    }
}
