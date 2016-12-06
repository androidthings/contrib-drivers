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

package com.google.android.things.contrib.driver.cap12xx;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.google.android.things.userdriver.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;

/**
 * User-space driver to process capacitive touch events from the
 * CAP12xx family of touch controllers and forward them to the
 * Android input framework.
 */
@SuppressWarnings("WeakerAccess")
public class Cap12xxInputDriver implements AutoCloseable {
    private static final String TAG = "Cap12xxInputDriver";

    // Driver parameters
    private static final String DRIVER_NAME = "Cap12xx";
    private static final int DRIVER_VERSION = 1;

    private Context mContext;
    private Cap12xx mPeripheralDevice;
    // Framework input driver
    private InputDriver mInputDriver;
    // Key codes mapped to input channels
    private int[] mKeycodes;

    /**
     * Create a new Cap12xxInputDriver to forward capacitive touch events
     * to the Android input framework.
     *
     * @param context Current context, used for loading resources.
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @param keyCodes {@link KeyEvent} codes to be emitted for each input channel.
     *                 Length must match the input channel count of the
     *                 touch controller.
     */
    public Cap12xxInputDriver(Context context,
                              String i2cName,
                              String alertName,
                              Cap12xx.Configuration chip,
                              int[] keyCodes) throws IOException {
        this(context, i2cName, alertName, chip, null, keyCodes);
    }

    /**
     * Create a new Cap12xxInputDriver to forward capacitive touch events
     * to the Android input framework.
     *
     * @param context Current context, used for loading resources.
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @param handler optional {@link Handler} for software polling and callback events.
     * @param keyCodes {@link KeyEvent} codes to be emitted for each input channel.
     *                 Length must match the input channel count of the
     *                 touch controller.
     */
    public Cap12xxInputDriver(Context context,
                              String i2cName,
                              String alertName,
                              Cap12xx.Configuration chip,
                              Handler handler,
                              int[] keyCodes) throws IOException {
        Cap12xx peripheral = new Cap12xx(context, i2cName, alertName, chip, handler);
        init(context, peripheral, keyCodes);
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ Cap12xxInputDriver(Context context,
                                   Cap12xx peripheral,
                                   int[] keyCodes) throws IOException {
        init(context, peripheral, keyCodes);
    }

    /**
     * Initialize peripheral defaults from the constructor.
     */
    private void init(Context context, Cap12xx peripheral, int[] keyCodes) {
        // Verify inputs
        if (keyCodes == null) {
            throw new IllegalArgumentException("Must provide a valid set of key codes.");
        }

        mKeycodes = keyCodes;
        mContext = context.getApplicationContext();
        mPeripheralDevice = peripheral;
        mPeripheralDevice.setOnCapTouchListener(mTouchListener);
    }

    /**
     * Set the repeat rate of generated events on active input channels.
     * Can be one of {@link Cap12xx#REPEAT_NORMAL}, {@link Cap12xx#REPEAT_FAST}, or
     * {@link Cap12xx#REPEAT_SLOW} to generate continuous events while any of
     * the input channels are actively detecting touch input.
     *
     * <p>Use {@link Cap12xx#REPEAT_DISABLE} to generate a single event for each
     * input touch and release. The default is {@link Cap12xx#REPEAT_NORMAL}.
     *
     * @param rate one of {@link Cap12xx#REPEAT_NORMAL}, {@link Cap12xx#REPEAT_SLOW},
     *             {@link Cap12xx#REPEAT_FAST}, or {@link Cap12xx#REPEAT_DISABLE}.
     *
     * @throws IOException
     */
    public void setRepeatRate(@Cap12xx.RepeatRate int rate) throws IOException {
        if (mPeripheralDevice != null) {
            mPeripheralDevice.setRepeatRate(rate);
        }
    }

    /**
     * Set the detection sensitivity of the capacitive input channels.
     *
     * <p>The default is {@link Cap12xx#SENSITIVITY_NORMAL}.
     *
     * @param sensitivity one of {@link Cap12xx#SENSITIVITY_NORMAL},
     *                    {@link Cap12xx#SENSITIVITY_LOW}, or {@link Cap12xx#SENSITIVITY_HIGH}.
     *
     * @throws IOException
     */
    public void setSensitivity(@Cap12xx.Sensitivity int sensitivity) throws IOException {
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
    private Cap12xx.OnCapTouchListener mTouchListener = new Cap12xx.OnCapTouchListener() {
        @Override
        public void onCapTouchEvent(Cap12xx controller, boolean[] inputStatus) {
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

        // Emit an event for each defined input channel
        for (int i = 0; i < mKeycodes.length; i++) {
            int keyAction = status[i] ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
            int keyCode = mKeycodes[i];
            mInputDriver.emit(new KeyEvent[]{
                    new KeyEvent(keyAction, keyCode)
            });
        }
    }

    /**
     * Register this driver with the Android input framework.
     */
    public void register() {
        if (mInputDriver == null) {
            UserDriverManager manager = UserDriverManager.getManager();
            mInputDriver = buildInputDriver();
            manager.registerInputDriver(mInputDriver);
        }
    }

    /**
     * Unregister this driver with the Android input framework.
     */
    public void unregister() {
        if (mInputDriver != null) {
            UserDriverManager manager = UserDriverManager.getManager();
            manager.unregisterInputDriver(mInputDriver);
            mInputDriver = null;
        }
    }

    /**
     * Returns the {@link InputDriver} instance this touch controller
     * uses to emit input events based on the driver's event codes list.
     */
    private InputDriver buildInputDriver() {
        return InputDriver.builder(InputDevice.SOURCE_CLASS_BUTTON)
                .setName(DRIVER_NAME)
                .setVersion(DRIVER_VERSION)
                .setKeys(mKeycodes)
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
