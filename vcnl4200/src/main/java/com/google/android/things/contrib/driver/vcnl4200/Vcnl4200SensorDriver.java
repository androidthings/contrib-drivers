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

package com.google.android.things.contrib.driver.vcnl4200;

import android.hardware.Sensor;

import com.google.android.things.contrib.driver.vcnl4200.Vcnl4200.Configuration;
import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.sensor.UserSensor;
import com.google.android.things.userdriver.sensor.UserSensorDriver;
import com.google.android.things.userdriver.sensor.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

public class Vcnl4200SensorDriver implements AutoCloseable {

    private static final String DRIVER_VENDOR = "Vishay Semiconductors";
    private static final String DRIVER_NAME = "VCNL4200";
    private static final int DRIVER_VERSION = 1;

    private ProximityUserDriver mProximityUserDriver;
    private AmbientLightUserDriver mAmbientLightUserDriver;

    private Vcnl4200 mDevice;

    /**
     * Create a new framework sensor driver connected on the given bus.
     * The driver emits {@link android.hardware.Sensor} with proximity and ambient light data when
     * registered.
     * @param bus I2C bus the sensor is connected to.
     * @param configuration Initial configuration of the sensor.
     * @throws IOException
     */
    public Vcnl4200SensorDriver(String bus, Configuration configuration) throws IOException {
        mDevice = new Vcnl4200(bus, configuration);
    }

    /**
     * Constructs sensor driver with default configuration.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Vcnl4200SensorDriver(String bus) throws IOException {
        this(bus, new Configuration.Builder().build());
    }

    /**
     * Register a {@link UserSensor} that pipes temperature readings into the Android SensorManager.
     * @see #unregisterProximitySensor()
     */
    public void registerProximitySensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mProximityUserDriver == null) {
            mProximityUserDriver = new ProximityUserDriver();
            UserDriverManager.getInstance().registerSensor(mProximityUserDriver.getUserSensor());
        }
    }

    /**
     * Unregister the proximity {@link UserSensor}.
     */
    public void unregisterProximitySensor() {
        if (mProximityUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mProximityUserDriver.getUserSensor());
            mProximityUserDriver = null;
        }
    }

    /**
     * Register a {@link UserSensor} that pipes temperature readings into the Android SensorManager.
     * @see #unregisterAmbientLightSensor()
     */
    public void registerAmbientLightSensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mAmbientLightUserDriver == null) {
            mAmbientLightUserDriver = new AmbientLightUserDriver();
            UserDriverManager.getInstance().registerSensor(mAmbientLightUserDriver.getUserSensor());
        }
    }

    /**
     * Unregister the proximity {@link UserSensor}.
     */
    public void unregisterAmbientLightSensor() {
        if (mAmbientLightUserDriver != null) {
            UserDriverManager.getInstance().unregisterSensor(mAmbientLightUserDriver.getUserSensor());
            mAmbientLightUserDriver = null;
        }
    }

    private class ProximityUserDriver implements UserSensorDriver {
        private static final float DRIVER_POWER = 800; // Max IRED driving current.
        private static final float DRIVER_RESOLUTION = 1.0f;  // Driver reports integer values
        private static final int DRIVER_MIN_DELAY_US = 5 * 1000; // For 1/160 duty cycle
        private static final int DRIVER_MAX_DELAY_US = 34 * 1000; // For 1/1280 duty cycle

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_PROXIMITY)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        // Note that max reported value may actually be 0xFFF, as the driver can be
                        // configured to report only 12-bit values.
                        .setMaxRange(mDevice.getCurrentPsMaxRange())
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
            return new UserSensorReading(new float[]{mDevice.getPsData()});
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {
            mEnabled = enabled;
            mDevice.enablePsPower(mEnabled);
        }

        private boolean isEnabled() {
            return mEnabled;
        }
    }

    private class AmbientLightUserDriver implements UserSensorDriver {
        private static final float DRIVER_POWER = 1; // ALS power consumption is around 213 uA.
        // The min and max delay for measurements is affected by the configured integration time.
        private static final int DRIVER_MIN_DELAY_US = 80 * 1000;
        private static final int DRIVER_MAX_DELAY_US = 640 * 1000;

        private boolean mEnabled;
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_LIGHT)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        // The max range and resolution is a function of the integration time
                        // setting of the device. Since the user sensor is registered once, users
                        // should not attempt to re-configure the device once registered.
                        .setMaxRange(mDevice.getCurrentAlsMaxRange())
                        .setResolution(mDevice.getCurrentAlsResolution())
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
            return new UserSensorReading(new float[]{mDevice.getAlsData()});
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {
            mEnabled = enabled;
            mDevice.enableAlsPower(mEnabled);
        }

        private boolean isEnabled() {
            return mEnabled;
        }
    }


    @Override
    public void close() throws Exception {
        unregisterProximitySensor();
        unregisterAmbientLightSensor();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }
}
