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

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for Raspberry Pi Sense HAT 5-button miniature joystick.
 *
 * @see <a href="https://github.com/raspberrypi/linux/blob/24e62728b3fc4f118c8ae17b374bce189bb188fc/drivers/input/joystick/rpisense-js.c">Sense HAT joystick Linux kernel driver</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Joystick implements AutoCloseable {

    private static final String TAG = "SenseHatJoystick";

    public static final int KEY_RELEASED = 0;
    public static final int KEY_PRESSED_DOWN = 1;
    public static final int KEY_PRESSED_RIGHT = 2;
    public static final int KEY_PRESSED_UP = 4;
    public static final int KEY_PRESSED_ENTER = 8;
    public static final int KEY_PRESSED_LEFT = 16;

    private static final int JOYSTICK_REG_KEYS = 0xF2;

    private I2cDevice mI2c;
    private Gpio mInterruptGpio;
    private OnButtonEventListener mListener;
    private int mPrevKey;

    /**
     * Interface definition for a callback to be invoked when a button event occurs.
     */
    public interface OnButtonEventListener {
        /**
         * Called when a button event occurs.
         *
         * @param key     the <code>KEY_PRESSED_*</code> key code of the button for which the event
         *                occurred
         * @param pressed <code>true</code> if the button is now pressed
         */
        void onButtonEvent(int key, boolean pressed);
    }

    /**
     * Creates a new joystick driver.
     *
     * @param bus          the I2C bus the joystick controller is connected to
     * @param address      the joystick controller I2C device slave address
     * @param interruptPin the interrupt GPIO pin the joystick controller is connected to
     * @throws IOException
     */
    public Joystick(String bus, int address, String interruptPin) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice i2c = pioService.openI2cDevice(bus, address);
        Gpio interruptGpio = pioService.openGpio(interruptPin);
        try {
            connect(i2c, interruptGpio);
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ Joystick(I2cDevice i2c, Gpio interruptGpio) throws IOException {
        connect(i2c, interruptGpio);
    }

    private void connect(I2cDevice i2c, Gpio interruptGpio) throws IOException {
        mI2c = i2c;
        mInterruptGpio = interruptGpio;
        mInterruptGpio.setDirection(Gpio.DIRECTION_IN);
        mInterruptGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
        mInterruptGpio.registerGpioCallback(mInterruptCallback);
    }

    /**
     * Local callback to monitor GPIO edge events.
     */
    private GpioCallback mInterruptCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                boolean trigger = gpio.getValue();
                if (trigger) {
                    int key = mI2c.readRegByte(JOYSTICK_REG_KEYS) & 0x7F;
                    if (key == KEY_RELEASED) {
                        performButtonEvent(mPrevKey, false);
                    } else {
                        performButtonEvent(key, true);
                    }
                    mPrevKey = key;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading button state", e);
            }

            return true;
        }
    };

    /**
     * Sets the listener to be called when a button event is occurred.
     *
     * @param listener button event listener to be invoked
     */
    public void setOnButtonEventListener(OnButtonEventListener listener) {
        mListener = listener;
    }

    /**
     * Closes the driver and its underlying device.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        mListener = null;

        if (mI2c != null) {
            try {
                mI2c.close();
            } finally {
                mI2c = null;
            }
        }

        if (mInterruptGpio != null) {
            mInterruptGpio.unregisterGpioCallback(mInterruptCallback);
            try {
                mInterruptGpio.close();
            } finally {
                mInterruptGpio = null;
            }
        }
    }

    /**
     * Invokes button event callback.
     */
    private void performButtonEvent(int key, boolean state) {
        if (mListener != null) {
            mListener.onButtonEvent(key, state);
        }
    }

}
