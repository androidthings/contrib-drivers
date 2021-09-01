/*
 * Copyright 2017 Google Inc.
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

package com.google.android.things.contrib.driver.bh1750;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.UserSensor;
import com.google.android.things.userdriver.UserSensorDriver;
import com.google.android.things.userdriver.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

/**
 * Driver for the BH1750 digital light sensor.
 */
public class Bh1750SensorDriver implements AutoCloseable {

    private static final String TAG = Bh1750SensorDriver.class.getSimpleName();

    // DRIVER parameters
    // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
    private static final String DRIVER_VENDOR = "ROHM";
    private static final String DRIVER_NAME = "BH1750";
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.f / Bh1750.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.f / Bh1750.MIN_FREQ_HZ);

    private Bh1750 mDevice;
    private LightUserDriver mLightUserDriver;

    /**
     * Create a new framework sensor driver connected on the given bus.
     * The driver emits {@link android.hardware.Sensor} with light data when
     * registered.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Bh1750SensorDriver(String bus) throws IOException {
        mDevice = new Bh1750(bus);
    }

    /**
     * Create a new framework sensor driver connected on the given bus and address.
     * The driver emits {@link android.hardware.Sensor} with light data when
     * registered.
     * @param bus I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public Bh1750SensorDriver(String bus, int address) throws IOException {
        mDevice = new Bh1750(bus, address);
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        unregisterLightSensor();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Register a {@link UserSensor} that pipes light level readings into the Android SensorManager.
     * @see #registerLightSensor()
     */
    public void registerLightSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mLightUserDriver == null) {
            mLightUserDriver = new LightUserDriver();
            UserDriverManager.getManager().registerSensor(mLightUserDriver.getUserSensor());
        }
    }

    /**
     * Unregister the light {@link UserSensor}.
     */
    public void unregisterLightSensor() {
        if (mLightUserDriver != null) {
            UserDriverManager.getManager().unregisterSensor(mLightUserDriver.getUserSensor());
            mLightUserDriver = null;
        }
    }

    private void maybeSleep() throws IOException {
        if (mLightUserDriver == null || !mLightUserDriver.isEnabled()) {
            mDevice.setMode(Bh1750.POWER_DOWN);
        } else {
            mDevice.setMode(Bh1750.POWER_ON);
        }
    }

    private class LightUserDriver extends UserSensorDriver {
        // DRIVER parameters
        // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
        private static final float DRIVER_MAX_RANGE = Bh1750.MAX_LIGHT_LX;
        private static final float DRIVER_RESOLUTION = 0.5f;
        private static final float DRIVER_POWER = Bh1750.MAX_POWER_CONSUMPTION_UA / 1000.f;
        private static final int DRIVER_VERSION = 1;
        private static final String DRIVER_REQUIRED_PERMISSION = "";

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_LIGHT)
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
            return new UserSensorReading(new float[]{mDevice.readLightLevel()});
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

