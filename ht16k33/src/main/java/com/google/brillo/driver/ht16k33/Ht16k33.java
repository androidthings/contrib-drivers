package com.google.brillo.driver.ht16k33;

import android.hardware.pio.I2cDevice;
import android.hardware.pio.PeripheralManagerService;
import android.system.ErrnoException;

import java.io.Closeable;
import java.nio.ByteBuffer;

import android.util.Log;


public class Ht16k33 implements Closeable {
    private static final String TAG = "Ht16k33";

    /**
     * I2C slave address.
     */
    public static final int I2C_ADDRESS = 0x70;

    private static final int HT16K33_CMD_SYSTEM_SETUP = 0x20;
    private static final int HT16K33_OSCILLATOR_ON = 0b0001;
    private static final int HT16K33_OSCILLATOR_OFF = 0b0000;
    private static final int HT16K33_CMD_DISPLAYSETUP = 0x80;
    private static final int HT16K33_DISPLAY_ON = 0b0001;
    private static final int HT16K33_DISPLAY_OFF = 0b0000;
    private static final int HT16K33_CMD_BRIGHTNESS = 0xE0;
    static final int HT16K33_BRIGHTNESS_MAX = 0b00001111;

    private I2cDevice mDevice;

    /**
     * Create a new driver for a HT16K33 peripheral connected on the given I2C bus.
     * @param bus
     */
    public Ht16k33(String bus) throws ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
        connect(device);
    }

    /**
     * Create a new driver for a HT16K33 peripheral from a given I2C device.
     * @param device
     */
    public Ht16k33(I2cDevice device) {
        connect(device);
    }

    private void connect(I2cDevice device) {
        mDevice = device;
    }


    /**
     * Close the device and the underlying device.
     */
    @Override
    public void close() {
        mDevice.close();
    }

    /**
     * Enable oscillator and LED display.
     * @throws ErrnoException
     */
    public void setEnabled(boolean enabled) throws ErrnoException {
        int oscillator_flag = enabled ? HT16K33_OSCILLATOR_ON : HT16K33_OSCILLATOR_OFF;
        mDevice.write(new byte[]{(byte) (HT16K33_CMD_SYSTEM_SETUP|oscillator_flag)}, 1);
        int display_flag = enabled ? HT16K33_DISPLAY_ON : HT16K33_DISPLAY_OFF;
        mDevice.write(new byte[]{(byte) (HT16K33_CMD_DISPLAYSETUP|display_flag)}, 1);
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and 16
     */
    public void setBrightness(int value) throws ErrnoException {
        if (value < 0 || value > HT16K33_BRIGHTNESS_MAX) {
            throw new IllegalArgumentException("brightness must be between 0 and " +
                    HT16K33_BRIGHTNESS_MAX);
        }
        mDevice.write(new byte[]{(byte)(HT16K33_CMD_BRIGHTNESS|(byte)value)}, 1);
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and 1.0f
     */
    public void setBrightness(float value) throws ErrnoException {
        int val = Math.round(value*Ht16k33.HT16K33_BRIGHTNESS_MAX);
        setBrightness(val);
    }

    /***
     * Write 16bit of LED row data to the given column.
     * @param column
     * @param data LED state for ROW0-15
     */
    public void writeColumn(int column, short data) throws ErrnoException {
        mDevice.writeRegWord(column*2, data);
    }
}
