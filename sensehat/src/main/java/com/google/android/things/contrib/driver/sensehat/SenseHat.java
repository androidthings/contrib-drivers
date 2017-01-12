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

package com.google.android.things.contrib.driver.sensehat;

import com.google.android.things.contrib.driver.hts221.Hts221;
import com.google.android.things.contrib.driver.hts221.Hts221SensorDriver;
import com.google.android.things.contrib.driver.lps25h.Lps25h;
import com.google.android.things.contrib.driver.lps25h.Lps25hSensorDriver;

import java.io.IOException;

/**
 * Driver factory for the Sense Hat.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SenseHat {

    public static final String I2C_BUS = "I2C1";
    public static final int I2C_ADDRESS = 0x46;

    public static final int DISPLAY_WIDTH = LedMatrix.WIDTH;
    public static final int DISPLAY_HEIGHT = LedMatrix.HEIGHT;

    public static final String JOYSTICK_INTERRUPT = "BCM23"; // Interrupt pin for joystick events

    // 8×8 RGB LED matrix

    public static LedMatrix openDisplay() throws IOException {
        return new LedMatrix(I2C_BUS);
    }

    // 5-button miniature joystick

    public static Joystick openJoystick() throws IOException {
        return new Joystick(I2C_BUS, I2C_ADDRESS, JOYSTICK_INTERRUPT);
    }

    public static JoystickDriver createJoystickDriver() throws IOException {
        return new JoystickDriver(I2C_BUS, I2C_ADDRESS, JOYSTICK_INTERRUPT);
    }

    // ST LPS25H barometric pressure and temperature sensor

    public static Lps25h openPressureSensor() throws IOException {
        return new Lps25h(I2C_BUS);
    }

    public static Lps25hSensorDriver createPressureSensorDriver() throws IOException {
        return new Lps25hSensorDriver(I2C_BUS);
    }

    // ST HTS221 relative humidity and temperature sensor

    public static Hts221 openHumiditySensor() throws IOException {
        return new Hts221(I2C_BUS);
    }

    public static Hts221SensorDriver createHumiditySensorDriver() throws IOException {
        return new Hts221SensorDriver(I2C_BUS);
    }

}
