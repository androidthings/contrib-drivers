/*
 * Copyright 2016 Google Inc.
 * Copyright 2017 Macro Yau
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

import android.graphics.Color;

import com.google.android.things.contrib.driver.hts221.Hts221;
import com.google.android.things.contrib.driver.hts221.Hts221SensorDriver;
import com.google.android.things.contrib.driver.lps25h.Lps25h;
import com.google.android.things.contrib.driver.lps25h.Lps25hSensorDriver;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver factory for Raspberry Pi Sense HAT.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SenseHat implements AutoCloseable {

    public static final int DISPLAY_WIDTH = LedMatrix.WIDTH;
    public static final int DISPLAY_HEIGHT = LedMatrix.HEIGHT;

    private static final String I2C_BUS = "I2C1";
    private static final int I2C_ADDRESS = 0x46;

    private static final String JOYSTICK_INTERRUPT = "BCM23"; // Interrupt pin for joystick events

    private static final int SENSE_HAT_REG_WHO_AM_I = 0xF0;

    protected static I2cDevice i2cDevice;

    private LedMatrix mLedMatrix;
    private Joystick mJoystick;
    private JoystickDriver mJoystickDriver;

    public SenseHat() throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        i2cDevice = pioService.openI2cDevice(I2C_BUS, I2C_ADDRESS);

        if (!isAttached()) {
            throw new IOException("Sense HAT is not attached");
        }
    }

    @Override
    public void close() throws IOException {
        closeDisplay();
        closeJoystick();
        closeJoystickDriver();

        if (i2cDevice != null) {
            try {
                i2cDevice.close();
            } finally {
                i2cDevice = null;
            }
        }
    }

    /**
     * Checks whether Sense HAT is attached to Raspberry Pi correctly.
     *
     * @return <code>true</code> if attached
     * @throws IOException
     */
    private boolean isAttached() throws IOException {
        return i2cDevice != null && i2cDevice.readRegByte(SENSE_HAT_REG_WHO_AM_I) == 's';
    }

    // 8×8 RGB LED matrix

    public LedMatrix openDisplay() throws IOException {
        if (mLedMatrix == null) {
            mLedMatrix = new LedMatrix();
        }

        return mLedMatrix;
    }

    public void closeDisplay() throws IOException {
        if (mLedMatrix != null) {
            try {
                mLedMatrix.draw(Color.BLACK);
            } finally {
                mLedMatrix = null;
            }
        }
    }

    // 5-button miniature joystick

    public Joystick openJoystick() throws IOException {
        if (mJoystick == null) {
            mJoystick = new Joystick(JOYSTICK_INTERRUPT);
        }

        return mJoystick;
    }

    public void closeJoystick() throws IOException {
        if (mJoystick != null) {
            try {
                mJoystick.close();
            } finally {
                mJoystick = null;
            }
        }
    }

    public JoystickDriver createJoystickDriver() throws IOException {
        if (mJoystickDriver == null) {
            mJoystickDriver = new JoystickDriver(JOYSTICK_INTERRUPT);
        }

        return mJoystickDriver;
    }

    public void closeJoystickDriver() throws IOException {
        if (mJoystickDriver != null) {
            try {
                mJoystickDriver.close();
            } finally {
                mJoystickDriver = null;
            }
        }
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
