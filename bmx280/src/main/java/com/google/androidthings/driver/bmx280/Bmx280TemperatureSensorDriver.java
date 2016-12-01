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

package com.google.androidthings.driver.bmx280;

import android.hardware.Sensor;

import com.google.androidthings.userdriver.UserDriverManager;
import com.google.androidthings.userdriver.UserSensor;
import com.google.androidthings.userdriver.UserSensorDriver;
import com.google.androidthings.userdriver.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

public class Bmx280TemperatureSensorDriver implements AutoCloseable {
    private static final String TAG = "TemperatureSensorDriver";
    // DRIVER parameters
    // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
    private static final String DRIVER_NAME = "BMP280/BME280";
    private static final String DRIVER_VENDOR = "Bosch";
    private static final float DRIVER_MAX_RANGE = Bmx280.MAX_TEMP_C;
    private static final float DRIVER_RESOLUTION = 0.005f;
    private static final float DRIVER_POWER = Bmx280.MAX_POWER_CONSUMPTION_TEMP_UA / 1000.f;
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.f/Bmx280.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.f/Bmx280.MIN_FREQ_HZ);
    private static final int DRIVER_VERSION = 1;
    private static final String DRIVER_REQUIRED_PERMISSION = "";

    private Bmx280 mDevice;
    private UserSensor mUserSensor;

    /**
     * Create a new framework pressure sensor driver connected on the given bus.
     * The driver emits {@link android.hardware.Sensor} with temperature data when registered.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     * @see #register()
     */
    public Bmx280TemperatureSensorDriver(String bus) throws IOException {
        mDevice = new Bmx280(bus);
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregister();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Register the driver in the framework.
     * @see #unregister()
     */
    public void register() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot registered closed driver");
        }
        if (mUserSensor == null) {
            mUserSensor = build(mDevice);
            UserDriverManager.getManager().registerSensor(mUserSensor);
        }
    }

    /**
     * Unregister the driver from the framework.
     */
    public void unregister() {
        if (mUserSensor != null) {
            UserDriverManager.getManager().unregisterSensor(mUserSensor);
            mUserSensor = null;
        }
    }

    static UserSensor build(final Bmx280 device) {
        return UserSensor.builder()
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
                .setDriver(new UserSensorDriver() {
                    @Override
                    public UserSensorReading read() throws IOException {
                        return new UserSensorReading(new float[]{device.readTemperature()});
                    }

                    @Override
                    public void setEnabled(boolean enabled) throws IOException {
                        if (enabled) {
                            device.setMode(Bmx280.MODE_NORMAL);
                            device.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
                        } else {
                            device.setMode(Bmx280.MODE_SLEEP);
                            device.setTemperatureOversampling(Bmx280.OVERSAMPLING_SKIPPED);
                        }
                    }
                })
                .build();
    }
}
