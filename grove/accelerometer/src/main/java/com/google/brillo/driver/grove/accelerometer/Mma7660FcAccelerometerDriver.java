package com.google.brillo.driver.grove.accelerometer;

import android.hardware.SensorManager;
import android.hardware.userdriver.sensors.AccelerometerDriver;
import android.hardware.userdriver.sensors.VectorWithStatus;
import android.system.ErrnoException;
import android.util.Log;

import java.util.UUID;

class Mma7660FcAccelerometerDriver {
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

    static AccelerometerDriver build(Mma7660Fc mma7660fc) {
        return new AccelerometerDriver(DRIVER_NAME, DRIVER_VENDOR, DRIVER_VERSION,
                DRIVER_MAX_RANGE, DRIVER_RESOLUTION, DRIVER_POWER,
                DRIVER_MIN_DELAY_US, DRIVER_REQUIRED_PERMISSION, DRIVER_MAX_DELAY_US,
                UUID.randomUUID()) {

            @Override
            public void enable(boolean enable) {
                try {
                    if (enable) {
                        mma7660fc.setMode(Mma7660Fc.MODE_ACTIVE);
                    } else {
                        mma7660fc.setMode(Mma7660Fc.MODE_STANDBY);
                    }
                } catch (ErrnoException e) {
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
                } catch (ErrnoException | IllegalStateException e) {
                    Log.e(TAG, "peripheral error: ", e);
                    return new VectorWithStatus(0, 0, 0, SensorManager.SENSOR_STATUS_UNRELIABLE);
                }
            }
        };
    }
}
