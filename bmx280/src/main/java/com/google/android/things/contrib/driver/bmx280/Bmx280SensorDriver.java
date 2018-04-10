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

package com.google.android.things.contrib.driver.bmx280;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.sensor.UserSensor;
import com.google.android.things.userdriver.sensor.UserSensorDriver;
import com.google.android.things.userdriver.sensor.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

public class Bmx280SensorDriver implements AutoCloseable {
    private static final String TAG = "Bmx280SensorDriver";

    // DRIVER parameters
    // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
    private static final String DRIVER_VENDOR = "Bosch";
    private static final String DRIVER_NAME = "BMP280/BME280";
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.f / Bmx280.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.f / Bmx280.MIN_FREQ_HZ);

    private Bmx280 mDevice;

    private TemperatureUserDriver mTemperatureUserDriver;
    private PressureUserDriver mPressureUserDriver;
    private HumidityUserDriver mHumidityUserDriver;

    /**
     * Create a new framework sensor driver connected on the given bus.
     * The driver emits {@link android.hardware.Sensor} with pressure and temperature data when
     * registered.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     * @see #registerPressureSensor()
     * @see #registerTemperatureSensor()
     */
    public Bmx280SensorDriver(String bus) throws IOException {
        mDevice = new Bmx280(bus);
    }

    /**
     * Create a new framework sensor driver connected on the given bus and address.
     * The driver emits {@link android.hardware.Sensor} with pressure and temperature data when
     * registered.
     * @param bus I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     * @see #registerPressureSensor()
     * @see #registerTemperatureSensor()
     */
    public Bmx280SensorDriver(String bus, int address) throws IOException {
        mDevice = new Bmx280(bus, address);
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregisterTemperatureSensor();
        unregisterPressureSensor();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Register a {@link UserSensor} that pipes temperature readings into the Android SensorManager.
     * @see #unregisterTemperatureSensor()
     */
    public void registerTemperatureSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mTemperatureUserDriver == null) {
            mTemperatureUserDriver = new TemperatureUserDriver();
            UserDriverManager.getInstance().registerSensor(mTemperatureUserDriver.getUserSensor());
        }
    }

    /**
     * Register a {@link UserSensor} that pipes pressure readings into the Android SensorManager.
     * @see #unregisterPressureSensor()
     */
    public void registerPressureSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mPressureUserDriver == null) {
            mPressureUserDriver = new PressureUserDriver();
            UserDriverManager.getInstance().registerSensor(mPressureUserDriver.getUserSensor());
        }
    }

    /**
     * Register a {@link UserSensor} that pipes humidity readings into the Android SensorManager.
     * @see #unregisterHumiditySensor()
     */
    public void registerHumiditySensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mHumidityUserDriver == null) {
            mHumidityUserDriver = new HumidityUserDriver();
            UserDriverManager.getInstance().registerSensor(mHumidityUserDriver.getUserSensor());
        }
    }

    /**
     * Unregister the temperature {@link UserSensor}.
     */
    public void unregisterTemperatureSensor() {
        if (mTemperatureUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mTemperatureUserDriver.getUserSensor());
            mTemperatureUserDriver = null;
        }
    }

    /**
     * Unregister the pressure {@link UserSensor}.
     */
    public void unregisterPressureSensor() {
        if (mPressureUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mPressureUserDriver.getUserSensor());
            mPressureUserDriver = null;
        }
    }

    /**
     * Unregister the humidity {@link UserSensor}.
     */
    public void unregisterHumiditySensor() {
        if (mHumidityUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mHumidityUserDriver.getUserSensor());
            mHumidityUserDriver = null;
        }
    }

    private void maybeSleep() throws IOException {
        if ((mTemperatureUserDriver == null || !mTemperatureUserDriver.isEnabled()) &&
            (mPressureUserDriver == null || !mPressureUserDriver.isEnabled()) &&
            (mHumidityUserDriver == null || !mHumidityUserDriver.isEnabled())) {
            mDevice.setMode(Bmx280.MODE_SLEEP);
        } else {
            mDevice.setMode(Bmx280.MODE_NORMAL);
        }
    }

    private class PressureUserDriver implements UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Bmx280.MAX_PRESSURE_HPA;
        private static final float DRIVER_RESOLUTION = .0262f;
        private static final float DRIVER_POWER = Bmx280.MAX_POWER_CONSUMPTION_PRESSURE_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_PRESSURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
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
            syncSamplingState();
            maybeSleep();
        }

        private boolean isEnabled() {
            return mEnabled;
        }
    }

    private class TemperatureUserDriver implements UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Bmx280.MAX_TEMP_C;
        private static final float DRIVER_RESOLUTION = 0.005f;
        private static final float DRIVER_POWER = Bmx280.MAX_POWER_CONSUMPTION_TEMP_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_AMBIENT_TEMPERATURE)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
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
            syncSamplingState();
            maybeSleep();
        }

        private boolean isEnabled() {
            return mEnabled;
        }
    }

    private class HumidityUserDriver implements UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Bmx280.MAX_HUM_RH;
        private static final float DRIVER_RESOLUTION = 0.005f;
        private static final float DRIVER_POWER = Bmx280.MAX_POWER_CONSUMPTION_HUMIDITY_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_RELATIVE_HUMIDITY)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readHumidity()});
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {
            mEnabled = enabled;
            syncSamplingState();
            maybeSleep();
        }

        private boolean isEnabled() {
            return mEnabled;
        }
    }

    private void syncSamplingState() throws IOException {
        // pressure and humidity both depend on temperature sampling
        boolean humidityEnabled = mHumidityUserDriver != null && mHumidityUserDriver.isEnabled();
        boolean pressureEnabled = mPressureUserDriver != null && mPressureUserDriver.isEnabled();
        boolean temperatureEnabled = humidityEnabled || pressureEnabled ||
                mTemperatureUserDriver != null && mTemperatureUserDriver.isEnabled();

        mDevice.setTemperatureOversampling(
                temperatureEnabled ? Bmx280.OVERSAMPLING_1X : Bmx280.OVERSAMPLING_SKIPPED);
        mDevice.setPressureOversampling(
                pressureEnabled ? Bmx280.OVERSAMPLING_1X : Bmx280.OVERSAMPLING_SKIPPED);
        if (mDevice.hasHumiditySensor()) {
            mDevice.setHumidityOversampling(
                    humidityEnabled ? Bmx280.OVERSAMPLING_1X : Bmx280.OVERSAMPLING_SKIPPED);
        }
    }
}
