package com.google.brillo.driver.grove.lcdrgb;

import android.graphics.Color;
import android.hardware.pio.I2cDevice;
import android.hardware.pio.PeripheralManagerService;
import android.system.ErrnoException;

import java.io.Closeable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class LcdRgbBacklight implements Closeable {
    private static final int RGB_ADDRESS = 0xc4 >> 1;
    private static final int LCD_ADDRESS = 0x7c >> 1;
    private static final int REG_MODE_1 = 0x00;
    private static final int REG_OUTPUT = 0x08;
    private static final int REG_RED = 0x04;
    private static final int REG_GREEN = 0x03;
    private static final int REG_BLUE = 0x02;
    private static final int ENABLE_PWM = 0xff;
    private static final int ENABLE_BACKLIGHT = 0x00;
    private static final int SEND_COMMAND = 0x80;
    private static final int DISPLAY_ON = 0x04;
    private static final int DISPLAY_CONTROL = 0x08;
    private static final int ENTRY_LEFT = 0x02;
    private static final int ENTRY_MODE_SET = 0x04;
    private static final int CLEAR_DISPLAY = 0x01;
    private static final int RETURN_HOME = 0x02;
    private static final int WRITE_CHAR = 0x40;

    private static final byte[] COMMAND_DISPLAY_ON =
            {(byte) SEND_COMMAND, (byte) (DISPLAY_CONTROL | DISPLAY_ON)};
    private static final byte[] COMMAND_ENTRY_MODE =
            {(byte) SEND_COMMAND, (byte) (ENTRY_MODE_SET | ENTRY_LEFT)};
    private static final byte[] COMMAND_CLEAR = {(byte) SEND_COMMAND, (byte) CLEAR_DISPLAY};
    private static final byte[] COMMAND_HOME = {(byte) SEND_COMMAND, (byte) RETURN_HOME};

    private I2cDevice mRgbDevice;
    private I2cDevice mLcdDevice;


    public LcdRgbBacklight(String bus) throws ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mRgbDevice = pioService.openI2cDevice(bus, RGB_ADDRESS);
            mLcdDevice = pioService.openI2cDevice(bus,LCD_ADDRESS);
            mRgbDevice.writeRegByte(REG_MODE_1, (byte) ENABLE_BACKLIGHT);
            mRgbDevice.writeRegByte(REG_OUTPUT, (byte) ENABLE_PWM);
            mLcdDevice.write(COMMAND_DISPLAY_ON,COMMAND_DISPLAY_ON.length);
            mLcdDevice.write(COMMAND_ENTRY_MODE,COMMAND_ENTRY_MODE.length);
        } catch (ErrnoException|RuntimeException e) {
            close();
            throw e;
        }
    }

    public void close() {
        if (mRgbDevice != null) {
            mRgbDevice.close();
            mRgbDevice = null;
        }
        if (mLcdDevice != null) {
            mLcdDevice.close();
            mLcdDevice = null;
        }
    }

    public void clear() throws ErrnoException, IllegalStateException {
        if (mLcdDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mLcdDevice.write(COMMAND_CLEAR, COMMAND_CLEAR.length);
        mLcdDevice.write(COMMAND_HOME, COMMAND_HOME.length);
    }

    public void setBackground(int color) throws ErrnoException, IllegalStateException {
        if (mRgbDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mRgbDevice.writeRegByte(REG_RED, (byte)Color.red(color));
        mRgbDevice.writeRegByte(REG_GREEN, (byte)Color.green(color));
        mRgbDevice.writeRegByte(REG_BLUE, (byte)Color.blue(color));
    }

    public void write(String message) throws ErrnoException, IllegalStateException {
        if (mLcdDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mLcdDevice.write(COMMAND_HOME, COMMAND_HOME.length);
        for (byte c : message.getBytes()) {
            byte[] text = {(byte) WRITE_CHAR, c};
            mLcdDevice.write(text, text.length);
        }
    }

}
