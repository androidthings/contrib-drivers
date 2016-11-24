/*
 * Copyright 2016, The Android Open Source Project
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

package com.google.androidthings.driver.button;

import android.hardware.userdriver.InputDriver;
import android.hardware.userdriver.UserDriverManager;
import android.view.InputDevice;
import android.view.KeyEvent;

import java.io.IOException;

public class ButtonInputDriver implements AutoCloseable {
    private static final String DRIVER_NAME = "Button";
    private static final int DRIVER_VERSION = 1;

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
            throw new IllegalStateException("cannot registered closed driver");
        }
        if (mDriver == null) {
            mDriver = build(mDevice, mKeycode);
            UserDriverManager.getManager().registerInputDriver(mDriver);
        }
    }

    /**
     * Unregister the driver from the framework.
     */
    public void unregister() {
        if (mDriver != null) {
            UserDriverManager.getManager().registerInputDriver(mDriver);
            mDriver = null;
        }
    }

    static InputDriver build(Button button, int keyCode) {
        InputDriver inputDriver = InputDriver.builder(InputDevice.SOURCE_CLASS_BUTTON)
                .name(DRIVER_NAME)
                .version(DRIVER_VERSION)
                .keys(new int[]{keyCode})
                .build();
        button.setOnButtonEventListener(new Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(Button b, boolean pressed) {
                int keyAction = pressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
                inputDriver.emit(new KeyEvent[]{
                        new KeyEvent(keyAction, keyCode)
                });
            }
        });
        return inputDriver;
    }
}
