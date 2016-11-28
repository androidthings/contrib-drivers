/*
 * Copyright 2016, The Android Open Source Project
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

package com.google.androidthings.driver.mma7660fc;

import android.hardware.SensorManager;
import android.util.Log;

import com.google.androidthings.userdriver.UserDriverManager;
import com.google.androidthings.userdriver.sensors.AccelerometerDriver;
import com.google.androidthings.userdriver.sensors.VectorWithStatus;

import java.io.IOException;
import java.util.UUID;

public class Mma7660FcAccelerometerDriver implements AutoCloseable {
    private static final String TAG = Mma7660FcAccelerometerDriver.class.getSimpleName();
    private static final String DRIVER_NAME = "GroveAccelerometer";
    private static final String DRIVER_VENDOR = "Seeed";
    private static final float DRIVER_MAX_RANGE = Mma7660Fc.MAX_RANGE_G * SensorManager.GRAVITY_EARTH;
    private static final float DRIVER_RESOLUTION = DRIVER_MAX_RANGE / 32.f; // 6bit signed
    private static final float DRIVER_POWER = Mma7660Fc.MAX_POWER_UA / 1000.f;
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.f/Mma7660Fc.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.f/Mma7660Fc.MIN_FREQ_HZ);
    private static final int DRIVER_VERSION = 1;
    private static final String DRIVER_REQUIRED_PERMISSION = "";
    private Mma7660Fc mDevice;
    private AccelerometerDriver mDriver;

    /**
     * Create a new framework accelerometer driver connected to the given I2C bus.
     * The driver emits {@link android.hardware.Sensor} with acceleration data when registered.
     * @param bus
     * @throws IOException
     * @see #register()
     */
    public Mma7660FcAccelerometerDriver(String bus) throws IOException {
        mDevice = new Mma7660Fc(bus);
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
        if (mDriver == null) {
            mDriver = build(mDevice);
            UserDriverManager.getManager().registerSensorDriver(mDriver);
        }
    }

    /**
     * Unregister the driver from the framework.
     */
    public void unregister() {
        if (mDriver != null) {
            UserDriverManager.getManager().unregisterSensorDriver(mDriver);
            mDriver = null;
        }
    }

    static AccelerometerDriver build(Mma7660Fc mma7660fc) {
        return new AccelerometerDriver(DRIVER_NAME, DRIVER_VENDOR, DRIVER_VERSION,
                DRIVER_MAX_RANGE, DRIVER_RESOLUTION, DRIVER_POWER,
                DRIVER_MIN_DELAY_US, DRIVER_REQUIRED_PERMISSION, DRIVER_MAX_DELAY_US,
                UUID.randomUUID()) {

            @Override
            public void setEnabled(boolean enabled) throws IOException {
                try {
                    if (enabled) {
                        mma7660fc.setMode(Mma7660Fc.MODE_ACTIVE);
                    } else {
                        mma7660fc.setMode(Mma7660Fc.MODE_STANDBY);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "peripheral error: ", e);
                }
            }

            @Override
            public VectorWithStatus read() {
                try {
                    float[] sample = mma7660fc.readSample();
                    return new VectorWithStatus(
                            sample[0] * SensorManager.GRAVITY_EARTH,
                            sample[1] * SensorManager.GRAVITY_EARTH,
                            sample[2] * SensorManager.GRAVITY_EARTH,
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH); // 120Hz
                } catch (IOException | IllegalStateException e) {
                    Log.e(TAG, "peripheral error: ", e);
                    return new VectorWithStatus(0, 0, 0, SensorManager.SENSOR_STATUS_UNRELIABLE);
                }
            }
        };
    }
}
