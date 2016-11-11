package com.google.brillo.driver.bmx280;

import android.hardware.userdriver.UserDriverManager;
import android.hardware.userdriver.sensors.PressureSensorDriver;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class Bmx280PressureSensorDriver implements AutoCloseable {
    private static final String TAG = "PressureSensorDriver";
    // DRIVER parameters
    // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
    private static final String DRIVER_NAME = "BMP280/BME280";
    private static final String DRIVER_VENDOR = "Bosch";
    private static final float DRIVER_MAX_RANGE = Bmx280.MAX_PRESSURE_HPA;
    private static final float DRIVER_RESOLUTION = .0262f;
    private static final float DRIVER_POWER = Bmx280.MAX_POWER_CONSUMPTION_PRESSURE_UA / 1000.f;
    private static final int DRIVER_MIN_DELAY_US = Math.round(1000000.f / Bmx280.MAX_FREQ_HZ);
    private static final int DRIVER_MAX_DELAY_US = Math.round(1000000.f / Bmx280.MIN_FREQ_HZ);
    private static final int DRIVER_VERSION = 1;
    private static final String DRIVER_REQUIRED_PERMISSION = "";

    private Bmx280 mDevice;
    private PressureSensorDriver mDriver;

    /**
     * Create a new framework pressure sensor driver connected on the given bus.
     * The driver emits {@link android.hardware.Sensor} with pressure data when registered.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     * @see #register()
     */
    public Bmx280PressureSensorDriver(String bus) throws IOException {
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

    static PressureSensorDriver build(Bmx280 driver) {
        return new PressureSensorDriver(DRIVER_NAME, DRIVER_VENDOR, DRIVER_VERSION,
                DRIVER_MAX_RANGE, DRIVER_RESOLUTION, DRIVER_POWER, DRIVER_MIN_DELAY_US,
                DRIVER_REQUIRED_PERMISSION, DRIVER_MAX_DELAY_US, UUID.randomUUID()) {
            @Override
            public float read() {
                try {
                    return driver.readPressure();
                } catch (IOException | IllegalStateException e) {
                    Log.e(TAG, "Error reading pressure", e);
                    return Float.NaN;
                }
            }
        };
    }
}
