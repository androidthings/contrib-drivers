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

import com.google.android.things.contrib.driver.cap1xxx.Cap1xxx;

import java.io.IOException;

/**
 * Driver for the Microchip CAP12xx Capacitive Touch Controller
 * e.g. http://www.microchip.com/wwwproducts/en/CAP1208
 * @deprecated Use {@link Cap1xxx} instead.
 */
@Deprecated
@SuppressWarnings("WeakerAccess")
public class Cap12xx extends Cap1xxx {
    private static final String TAG = "Cap12xx";

    /**
     * @deprecated Use {@link #Cap12xx(String, String, Configuration)} instead.
     */
    @Deprecated
    public Cap12xx(Context context, String i2cName, String alertName, Configuration chip) throws IOException {
        this(i2cName, alertName, chip, null);
    }

    /**
     * Create a new Cap1xxx controller.
     *
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @throws IOException
     */
    public Cap12xx(String i2cName, String alertName, Configuration chip) throws IOException {
        this(i2cName, alertName, chip, null);
    }

    /**
     * @deprecated Use {@link #Cap12xx(String, String, Configuration, Handler)} instead.
     */
    @Deprecated
    public Cap12xx(Context context, String i2cName, String alertName, Configuration chip,
            Handler handler) throws IOException {
        this(i2cName, alertName, chip, handler);
    }

    /**
     * Create a new Cap1xxx controller.
     *
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @param handler optional {@link Handler} for software polling and callback events.
     * @throws IOException
     */
    public Cap12xx(String i2cName, String alertName, Configuration chip, Handler handler)
            throws IOException {
	super(i2cName, alertName, chip, handler);
    }
}
