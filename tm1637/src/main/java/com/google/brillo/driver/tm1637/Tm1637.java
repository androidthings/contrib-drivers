package com.google.brillo.driver.tm1637;

import android.hardware.pio.I2cDevice;
import android.hardware.pio.PeripheralManagerService;
import android.hardware.pio.Gpio;

import java.io.Closeable;
import java.io.IOException;


public class Tm1637 implements Closeable {
    private static final int TM1637_ADDR = 0x40;
    private static final int TM1637_REG = 0xc0;
    private static final int TM1637_CMD = 0x88;
    private static final int TM1637_BRIGHTNESS_MAX = 0x07;
    private static final int TM1637_DATA_LENGTH_MAX = 4;

    I2cBitBangDevice mDevice;
    /**
     * Create a new driver for a TM1637 peripheral connected on the given GPIO pins.
     */
    public Tm1637(String dataPin, String clockPin) throws IOException {
        mDevice = new I2cBitBangDevice(TM1637_ADDR, dataPin, clockPin);
    }

    /**
     * Close the device and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and {@link #TM1637_BRIGHTNESS_MAX}
     */
    public void setBrightness(int value) throws IOException {
        if (value < 0 || value > TM1637_BRIGHTNESS_MAX) {
            throw new IllegalArgumentException("brightness must be between 0 and " +
                    TM1637_BRIGHTNESS_MAX);
        }
        mDevice.write(new byte[]{(byte)(TM1637_CMD|(byte)value)}, 1);
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and 1.0f
     */
    public void setBrightness(float value) throws IOException {
        int val = Math.round(value*TM1637_BRIGHTNESS_MAX);
        setBrightness(val);
    }

    /**
     * Write 4 bytes of LED data.
     */
    public void writeData(byte[] data) throws IOException {
        if (data.length > TM1637_DATA_LENGTH_MAX) {
            throw new IllegalArgumentException("data size should be less than " +
                    TM1637_DATA_LENGTH_MAX);
        }
        mDevice.writeRegBuffer(TM1637_REG, data, data.length);
    }
}
