package com.google.brillo.driver.grove.accelerometer;

import android.os.RemoteException;
import android.pio.I2cDevice;
import android.pio.PeripheralManagerService;
import android.system.ErrnoException;

import java.io.Closeable;

public class Accelerometer implements Closeable {
    private static final int ADDRESS = 0x4c;

    public enum SamplingRate {
        RATE_120HZ,
        RATE_64HZ,
        RATE_32HZ,
        RATE_16HZ,
        RATE_8HZ,
        RATE_4HZ,
        RATE_2HZ,
        RATE_1HZ
    }

    private enum Register {
        X,
        Y,
        Z,
        TILT,
        SAMPLING_RATE_STATUS,
        SLEEP_COUNT,
        INTERRUPT_SETUP,
        MODE,
        SAMPLING_RATE,
        TAP_DETECTION,
        TAP_DEBOUNCE_COUNT,
    }

    private I2cDevice mDevice;

    public enum Mode {
        STANDBY, // i2c on, output off, low power
        ACTIVE // i2c on, output on
    }

    public void open(String bus) throws RemoteException, ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        mDevice = pioService.openI2cDevice(bus, ADDRESS);
    }

    public void close() {
        if (mDevice != null) {
            mDevice.close();
            mDevice = null;
        }
    }

    public void setMode(Mode mode) throws RemoteException, ErrnoException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mDevice.writeRegByte(Register.MODE.ordinal(), mode.ordinal());
    }

    public void setSamplingRate(SamplingRate rate) throws RemoteException, ErrnoException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mDevice.writeRegByte(Register.SAMPLING_RATE.ordinal(), rate.ordinal());
    }

    public byte[] readSample() throws RemoteException, ErrnoException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        return mDevice.read(3);
    }
}
