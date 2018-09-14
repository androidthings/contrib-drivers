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
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx.Configuration;
import com.google.android.things.contrib.driver.cap1xxx.Cap1xxxInputDriver;

import java.io.IOException;

/**
 * User-space driver to process capacitive touch events from the
 * CAP12xx family of touch controllers and forward them to the
 * Android input framework.
 * @deprecated Use {@link Cap1xxxInputDriver} instead.
 */
@Deprecated
@SuppressWarnings("WeakerAccess")
public class Cap12xxInputDriver extends Cap1xxxInputDriver {
    private static final String TAG = "Cap12xxInputDriver";

    /**
     * @deprecated Use {@link #Cap12xxInputDriver(String, String, Configuration, int[])} instead.
     */
    @Deprecated
    public Cap12xxInputDriver(Context context, String i2cName, String alertName, Configuration chip,
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
    public Cap12xxInputDriver(String i2cName, String alertName, Configuration chip, int[] keyCodes)
            throws IOException {
        this(i2cName, alertName, chip, null, keyCodes);
    }

    /**
     * @deprecated Use {@link #Cap12xxInputDriver(String, String, Configuration, Handler, int[])} instead.
     */
    @Deprecated
    public Cap12xxInputDriver(Context context, String i2cName, String alertName, Configuration chip,
            Handler handler, int[] keyCodes) throws IOException {
        this(i2cName, alertName, chip, handler, keyCodes);
    }

    /**
     * Create a new Cap1xxxInputDriver to forward capacitive touch events to the Android input
     * framework.
     *
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName Optional GPIO pin name connected to the controller's alert interrupt signal.
     *                  Can be null.
     * @param chip Identifier for the connected controller device chip.
     * @param handler Optional {@link Handler} for software polling and callback events.
     * @param keyCodes {@link KeyEvent} codes to be emitted for each input channel. Length must
     *                 match the input channel count of the Configuration {@code chip}.
     */
    public Cap12xxInputDriver(String i2cName, String alertName, Configuration chip, Handler handler,
            int[] keyCodes) throws IOException {
        super(i2cName, alertName, chip, handler, keyCodes);
    }
}
