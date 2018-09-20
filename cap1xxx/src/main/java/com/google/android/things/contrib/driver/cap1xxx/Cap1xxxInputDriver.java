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

package com.google.android.things.contrib.driver.cap1xxx;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx.Configuration;
import com.google.android.things.userdriver.input.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import com.google.android.things.userdriver.input.InputDriverEvent;
import java.io.IOException;

/**
 * User-space driver to process capacitive touch events from the
 * CAP1xxx family of touch controllers and forward them to the
 * Android input framework.
 */
@SuppressWarnings("WeakerAccess")
public class Cap1xxxInputDriver implements AutoCloseable {
    private static final String TAG = "Cap1xxxInputDriver";

    // Driver parameters
    private static final String DRIVER_NAME = "Cap1xxx";

    private Cap1xxx mPeripheralDevice;
    // Framework input driver
    private InputDriver mInputDriver;
    // Key codes mapped to input channels
    private int[] mKeycodes;

    /**
     * @deprecated Use {@link #Cap1xxxInputDriver(String, String, Configuration, int[])} instead.
     */
    @Deprecated
    public Cap1xxxInputDriver(Context context, String i2cName, String alertName, Configuration chip,
            int[] keyCodes) throws IOException {
        this(i2cName, alertName, chip, null, keyCodes);
    }

    /**
     * Create a new Cap1xxxInputDriver to forward capacitive touch events to the Android input
     * framework.
     *
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName Optional GPIO pin name connected to the controller's alert interrupt signal.
     *                  Can be null.
     * @param chip Identifier for the connected controller device chip.
     * @param keyCodes {@link KeyEvent} codes to be emitted for each input channel. Length must
     *                 match the input channel count of the Configuration {@code chip}.
     */
    public Cap1xxxInputDriver(String i2cName, String alertName, Configuration chip, int[] keyCodes)
            throws IOException {
        this(i2cName, alertName, chip, null, keyCodes);
    }

    /**
     * @deprecated Use {@link #Cap1xxxInputDriver(String, String, Configuration, Handler, int[])} instead.
     */
    @Deprecated
    public Cap1xxxInputDriver(Context context, String i2cName, String alertName, Configuration chip,
            Handler handler, int[] keyCodes) throws IOException {
        this(i2cName, alertName, chip, handler, keyCodes);
    }

    /**
     * Create a new Cap1xxxInputDriver with the default I2C address to forward capacitive touch
     * events to the Android input framework.
     *
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName Optional GPIO pin name connected to the controller's alert interrupt signal.
     *                  Can be null.
     * @param chip Identifier for the connected controller device chip.
     * @param handler Optional {@link Handler} for software polling and callback events.
     * @param keyCodes {@link KeyEvent} codes to be emitted for each input channel. Length must
     *                 match the input channel count of the Configuration {@code chip}.
     */
    public Cap1xxxInputDriver(String i2cName, String alertName, Configuration chip, Handler handler,
            int[] keyCodes) throws IOException {
        Cap1xxx peripheral = new Cap1xxx(i2cName, alertName, chip, handler);
        init(peripheral, keyCodes);
    }

    /**
     * Create a new Cap1xxxInputDriver to forward capacitive touch events to the Android input
     * framework.
     *
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param i2cAddress 7-bit I2C address for the attached controller.
     * @param alertName Optional GPIO pin name connected to the controller's alert interrupt signal.
     *                  Can be null.
     * @param chip Identifier for the connected controller device chip.
     * @param handler Optional {@link Handler} for software polling and callback events.
     * @param keyCodes {@link KeyEvent} codes to be emitted for each input channel. Length must
     *                 match the input channel count of the Configuration {@code chip}.
     */
    public Cap1xxxInputDriver(String i2cName, int i2cAddress, String alertName, Configuration chip, Handler handler,
                              int[] keyCodes) throws IOException {
        Cap1xxx peripheral = new Cap1xxx(i2cName, i2cAddress, alertName, chip, handler);
        init(peripheral, keyCodes);
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ Cap1xxxInputDriver(Cap1xxx peripheral, int[] keyCodes) throws IOException {
        init(peripheral, keyCodes);
    }

    /**
     * Initialize peripheral defaults from the constructor.
     */
    private void init(Cap1xxx peripheral, int[] keyCodes) {
        // Verify inputs
        if (keyCodes == null) {
            throw new IllegalArgumentException("Must provide a valid set of key codes.");
        }

        mKeycodes = keyCodes;
        mPeripheralDevice = peripheral;
        mPeripheralDevice.setOnCapTouchListener(mTouchListener);
    }

    /**
     * Set the repeat rate of generated events on active input channels.
     * Can be one of {@link Cap1xxx#REPEAT_NORMAL}, {@link Cap1xxx#REPEAT_FAST}, or
     * {@link Cap1xxx#REPEAT_SLOW} to generate continuous events while any of
     * the input channels are actively detecting touch input.
     *
     * <p>Use {@link Cap1xxx#REPEAT_DISABLE} to generate a single event for each
     * input touch and release. The default is {@link Cap1xxx#REPEAT_NORMAL}.
     *
     * @param rate one of {@link Cap1xxx#REPEAT_NORMAL}, {@link Cap1xxx#REPEAT_SLOW},
     *             {@link Cap1xxx#REPEAT_FAST}, or {@link Cap1xxx#REPEAT_DISABLE}.
     *
     * @throws IOException
     */
    public void setRepeatRate(@Cap1xxx.RepeatRate int rate) throws IOException {
        if (mPeripheralDevice != null) {
            mPeripheralDevice.setRepeatRate(rate);
        }
    }

    /**
     * Set the detection sensitivity of the capacitive input channels.
     *
     * <p>The default is {@link Cap1xxx#SENSITIVITY_NORMAL}.
     *
     * @param sensitivity one of {@link Cap1xxx#SENSITIVITY_NORMAL},
     *                    {@link Cap1xxx#SENSITIVITY_LOW}, or {@link Cap1xxx#SENSITIVITY_HIGH}.
     *
     * @throws IOException
     */
    public void setSensitivity(@Cap1xxx.Sensitivity int sensitivity) throws IOException {
        if (mPeripheralDevice != null) {
            mPeripheralDevice.setSensitivity(sensitivity);
        }
    }

    /**
     * Set the maximum number of input channels allowed to simultaneously
     * generate active events. Additional touches beyond this number will
     * be ignored.
     *
     * @param count Maximum number of inputs allowed to generate events.
     *
     * @throws IOException
     */
    public void setMultitouchInputMax(int count) throws IOException {
        if (mPeripheralDevice != null) {
            mPeripheralDevice.setMultitouchInputMax(count);
        }
    }

    /**
     * Callback invoked when touch events are received from
     * the peripheral device.
     */
    private Cap1xxx.OnCapTouchListener mTouchListener = new Cap1xxx.OnCapTouchListener() {
        @Override
        public void onCapTouchEvent(Cap1xxx controller, boolean[] inputStatus) {
            emitInputEvents(inputStatus);
        }
    };

    /**
     * Emit input events through the registered driver to the
     * Android input framework using the defined set of key codes.
     *
     * @param status Bitmask of input channel status flags.
     */
    private void emitInputEvents(boolean[] status) {
        if (mInputDriver == null) {
            Log.w(TAG, "Driver not yet registered");
            return;
        }

        InputDriverEvent inputEvent = new InputDriverEvent();
        // Emit an event for each defined input channel
        for (int i = 0; i < mKeycodes.length; i++) {
            int keyCode = mKeycodes[i];
            inputEvent.clear();
            inputEvent.setKeyPressed(keyCode, status[i]);
            mInputDriver.emit(inputEvent);
        }
    }

    /**
     * Register this driver with the Android input framework.
     */
    public void register() {
        if (mInputDriver == null) {
            UserDriverManager manager = UserDriverManager.getInstance();
            mInputDriver = buildInputDriver();
            manager.registerInputDriver(mInputDriver);
        }
    }

    /**
     * Unregister this driver with the Android input framework.
     */
    public void unregister() {
        if (mInputDriver != null) {
            UserDriverManager manager = UserDriverManager.getInstance();
            manager.unregisterInputDriver(mInputDriver);
            mInputDriver = null;
        }
    }

    /**
     * Returns the {@link InputDriver} instance this touch controller
     * uses to emit input events based on the driver's event codes list.
     */
    private InputDriver buildInputDriver() {
        return new InputDriver.Builder()
                .setName(DRIVER_NAME)
                .setSupportedKeys(mKeycodes)
                .build();
    }

    /**
     * Close this driver and any underlying resources associated with the connection.
     */
    @Override
    public void close() throws IOException {
        unregister();

        if (mPeripheralDevice != null) {
            mPeripheralDevice.setOnCapTouchListener(null);
            try {
                mPeripheralDevice.close();
            } finally {
                mPeripheralDevice = null;
            }
        }
    }
}
