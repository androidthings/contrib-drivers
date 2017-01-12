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

package com.google.android.things.contrib.driver.lps25h;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.UserSensor;
import com.google.android.things.userdriver.UserSensorDriver;
import com.google.android.things.userdriver.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

/**
 * User-space driver for interfacing the LPS25H pressure sensor to the Android SensorManager
 * framework.
 */
public class Lps25hSensorDriver implements AutoCloseable {

    private static final String DRIVER_VENDOR = "STMicroelectronics";
    private static final String DRIVER_NAME = "LPS25H";
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.0f / Lps25h.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.0f / Lps25h.MIN_FREQ_HZ);

    private Lps25h mDevice;

    private PressureUserDriver mPressureUserDriver;
    private TemperatureUserDriver mTemperatureUserDriver;

    /**
     * Creates a new framework sensor driver connected on the given bus.
     * The driver emits {@link Sensor} with pressure and temperature data when registered.
     *
     * @param bus the I2C bus the sensor is connected to
     * @throws IOException
     * @see #registerPressureSensor()
     * @see #registerTemperatureSensor()
     */
    public Lps25hSensorDriver(String bus) throws IOException {
        mDevice = new Lps25h(bus);
    }

    /**
     * Closes the driver and its underlying device.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregisterPressureSensor();
        unregisterTemperatureSensor();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Registers a {@link UserSensor} that pipes pressure readings into the Android SensorManager.
     *
     * @see #unregisterPressureSensor()
     */
    public void registerPressureSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("Cannot register closed driver");
        }

        if (mPressureUserDriver == null) {
            mPressureUserDriver = new PressureUserDriver();
            UserDriverManager.getManager().registerSensor(mPressureUserDriver.getUserSensor());
        }
    }

    /**
     * Registers a {@link UserSensor} that pipes temperature readings into the Android SensorManager.
     *
     * @see #unregisterTemperatureSensor()
     */
    public void registerTemperatureSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("Cannot register closed driver");
        }

        if (mTemperatureUserDriver == null) {
            mTemperatureUserDriver = new TemperatureUserDriver();
            UserDriverManager.getManager().registerSensor(mTemperatureUserDriver.getUserSensor());
        }
    }

    /**
     * Unregisters the pressure {@link UserSensor}.
     */
    public void unregisterPressureSensor() {
        if (mPressureUserDriver != null) {
            UserDriverManager.getManager().unregisterSensor(mPressureUserDriver.getUserSensor());
            mPressureUserDriver = null;
        }
    }

    /**
     * Unregisters the temperature {@link UserSensor}.
     */
    public void unregisterTemperatureSensor() {
        if (mTemperatureUserDriver != null) {
            UserDriverManager.getManager().unregisterSensor(mTemperatureUserDriver.getUserSensor());
            mTemperatureUserDriver = null;
        }
    }

    private void maybeSleep() throws IOException {
        if ((mTemperatureUserDriver == null || !mTemperatureUserDriver.isEnabled()) &&
                (mPressureUserDriver == null || !mPressureUserDriver.isEnabled())) {
            mDevice.setMode(Lps25h.MODE_POWER_DOWN);
        } else {
            mDevice.setMode(Lps25h.MODE_ACTIVE);
        }
    }

    private class PressureUserDriver extends UserSensorDriver {

        private static final float DRIVER_MAX_RANGE = Lps25h.MAX_PRESSURE_HPA;
        private static final float DRIVER_RESOLUTION = 0.0002f;
        private static final float DRIVER_POWER = Lps25h.MAX_POWER_CONSUMPTION_UA / 1000f;
        private static final int DRIVER_VERSION = 1;
        private static final String DRIVER_REQUIRED_PERMISSION = "";

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = UserSensor.builder()
                        .setType(Sensor.TYPE_PRESSURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setRequiredPermission(DRIVER_REQUIRED_PERMISSION)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readPressure()});
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {
            mEnabled = enabled;
            maybeSleep();
        }

        private boolean isEnabled() {
            return mEnabled;
        }

    }

    private class TemperatureUserDriver extends UserSensorDriver {

        private static final float DRIVER_MAX_RANGE = Lps25h.MAX_TEMP_C;
        private static final float DRIVER_RESOLUTION = 0.002f;
        private static final float DRIVER_POWER = Lps25h.MAX_POWER_CONSUMPTION_UA / 1000f;
        private static final int DRIVER_VERSION = 1;
        private static final String DRIVER_REQUIRED_PERMISSION = "";

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = UserSensor.builder()
                        .setType(Sensor.TYPE_AMBIENT_TEMPERATURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setRequiredPermission(DRIVER_REQUIRED_PERMISSION)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readTemperature()});
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {
            mEnabled = enabled;
            maybeSleep();
        }

        private boolean isEnabled() {
            return mEnabled;
        }

    }

}
