/*
 * Copyright 2016 Macro Yau
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

package com.google.android.things.contrib.driver.sensehat;

import android.view.InputDevice;
import android.view.KeyEvent;

import com.google.android.things.userdriver.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;

/**
 * User-space driver to process button events from the Raspberry Pi Sense HAT joystick and forward
 * them to the Android input framework.
 */
@SuppressWarnings("WeakerAccess")
public class JoystickDriver implements AutoCloseable {

    // Driver parameters
    private static final String DRIVER_NAME = "SenseHatJoystick";
    private static final int DRIVER_VERSION = 1;

    // Key code for driver to emulate
    private static final int KEY_CODE_UP = KeyEvent.KEYCODE_DPAD_UP;
    private static final int KEY_CODE_DOWN = KeyEvent.KEYCODE_DPAD_DOWN;
    private static final int KEY_CODE_LEFT = KeyEvent.KEYCODE_DPAD_LEFT;
    private static final int KEY_CODE_RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT;
    private static final int KEY_CODE_ENTER = KeyEvent.KEYCODE_DPAD_CENTER;

    private Joystick mDevice;
    private InputDriver mDriver;

    /**
     * Creates a new framework input driver for the joystick. The driver emits
     * {@link android.view.KeyEvent} with the directional pad key codes when registered.
     *
     * @param bus          the I2C bus the joystick controller is connected to
     * @param address      the joystick controller I2C device slave address
     * @param interruptPin the interrupt GPIO pin the joystick controller is connected to
     * @return the new input driver instance
     * @throws IOException
     * @see #register
     */
    public JoystickDriver(String bus, int address, String interruptPin) throws IOException {
        mDevice = new Joystick(bus, address, interruptPin);
    }

    /**
     * Closes the driver and its underlying device.
     *
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
     * Registers the driver in the framework.
     */
    public void register() {
        if (mDevice == null) {
            throw new IllegalStateException("Cannot registered closed driver");
        }

        if (mDriver == null) {
            mDriver = build(mDevice);
            UserDriverManager.getManager().registerInputDriver(mDriver);
        }
    }

    /**
     * Unregisters the driver from the framework.
     */
    public void unregister() {
        if (mDriver != null) {
            UserDriverManager.getManager().registerInputDriver(mDriver);
            mDriver = null;
        }
    }

    static InputDriver build(Joystick joystick) {
        final InputDriver inputDriver = InputDriver.builder(InputDevice.SOURCE_CLASS_BUTTON)
                .setName(DRIVER_NAME)
                .setVersion(DRIVER_VERSION)
                .setKeys(new int[]{KEY_CODE_UP, KEY_CODE_DOWN, KEY_CODE_LEFT, KEY_CODE_RIGHT, KEY_CODE_ENTER})
                .build();

        joystick.setOnButtonEventListener(new Joystick.OnButtonEventListener() {
            @Override
            public void onButtonEvent(int key, boolean pressed) {
                int keyAction = pressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
                int keyCode = -1;
                switch (key) {
                    case Joystick.KEY_PRESSED_UP:
                        keyCode = KEY_CODE_UP;
                        break;
                    case Joystick.KEY_PRESSED_DOWN:
                        keyCode = KEY_CODE_DOWN;
                        break;
                    case Joystick.KEY_PRESSED_LEFT:
                        keyCode = KEY_CODE_LEFT;
                        break;
                    case Joystick.KEY_PRESSED_RIGHT:
                        keyCode = KEY_CODE_RIGHT;
                        break;
                    case Joystick.KEY_PRESSED_ENTER:
                        keyCode = KEY_CODE_ENTER;
                        break;
                    default:
                        break;
                }
                if (keyCode != -1) {
                    inputDriver.emit(new KeyEvent[]{
                            new KeyEvent(keyAction, keyCode)
                    });
                }
            }
        });

        return inputDriver;
    }

}
