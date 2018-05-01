package com.google.android.things.contrib.driver.sensehat;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class BaroTemp {
    private final I2cDevice mDevice;

    /**
     * Create a new barometric pressure and temperature sensor driver connected on the given I2C bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException when a lower level does
     */
    public BaroTemp(String bus) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        mDevice = pioService.openI2cDevice(bus, SenseHat.I2C_LPS25H_ADDRESS);
    }
}
