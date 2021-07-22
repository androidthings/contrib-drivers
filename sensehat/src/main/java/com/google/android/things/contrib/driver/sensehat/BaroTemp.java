package com.google.android.things.contrib.driver.sensehat;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

/**
 * This class allows access to the LPS25H on the SenseHat.
 * <p>
 * See also: https://www.pololu.com/file/download/LPS25H.pdf?file_id=0J761</p>
 * <p>Source code referenced: https://github.com/tkurbad/mipSIE/blob/master/python/AltIMU-10v5/i2c.py</p>
 */
public class BaroTemp implements AutoCloseable {
    private I2cDevice mDevice;
    // Register addresses
    private final int LPS_REF_P_XL = 0x08; //  Reference pressure, lowest byte
    private final int LPS_REF_P_L = 0x09; //  Reference pressure, low byte
    private final int LPS_REF_P_H = 0x0A; //  Reference pressure, high byte

    private final int LPS_WHO_AM_I = 0x0F; //  Returns 0xbd (read only)

    private final int LPS_RES_CONF = 0x10; //  Set pressure and temperature resolution

    private final int LPS_CTRL_REG1 = 0x20; //  Set device power mode / ODR / BDU
    private final int LPS_CTRL_REG2 = 0x21; //  FIFO / I2C configuration
    private final int LPS_CTRL_REG3 = 0x22; //  Interrupt configuration
    private final int LPS_CTRL_REG4 = 0x23; //  Interrupt configuration

    private final int LPS_INTERRUPT_CFG = 0x24;//  Interrupt configuration
    private final int LPS_INT_SOURCE = 0x25; //  Interrupt source configuration

    private final int LPS_STATUS_REG = 0x27; //  Status (new pressure/temperature data available)

    private final int LPS_PRESS_OUT_XL = 0x28; //  Pressure output, loweste byte
    private final int LPS_PRESS_OUT_L = 0x29; //  Pressure output, low byte
    private final int LPS_PRESS_OUT_H = 0x2A; //  Pressure output, high byte

    private final int LPS_TEMP_OUT_L = 0x2B; //  Temperature output, low byte
    private final int LPS_TEMP_OUT_H = 0x2C; //  Temperature output, high byte

    private final int LPS_FIFO_CTRL = 0x2E; //  FIFO control / mode selection
    private final int LPS_FIFO_STATUS = 0x2F; //  FIFO status

    private final int LPS_THS_P_L = 0x30; //  Pressure interrupt threshold, low byte
    private final int LPS_THS_P_H = 0x31; //  Pressure interrupt threshold, high byte

    // The next two registers need special soldering
    private final int LPS_RPDS_L = 0x39;//  Pressure offset for differential pressure computing, low byte
    private final int LPS_RPDS_H = 0x3A; //  Differential offset, high byte
    private int mMillibarAdjust = 0;

    /**
     * Create a new barometric pressure and temperature sensor driver connected on the given I2C bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException when a lower level does
     */
    public BaroTemp(String bus) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        mDevice = pioService.openI2cDevice(bus, SenseHat.I2C_LPS25H_ADDRESS);
        // power down first
        mDevice.writeRegByte(LPS_CTRL_REG1, (byte) 0);
        // power up, data rate 12.5Hz (10110000)
        mDevice.writeRegByte(LPS_CTRL_REG1, (byte) 0xb0);
        if (0xBD != (mDevice.readRegByte(LPS_WHO_AM_I) & 0xFF)) {
            throw new IOException("This does not seem to be a LPS25H");
        }
    }

    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                // power down device
                mDevice.writeRegByte(LPS_CTRL_REG1, (byte) 0);
            } catch (Exception any) {
                // we tried
            }
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    private int readSigned24(int a0, int a1, int a2) throws IOException {
        int ret = (mDevice.readRegByte(a0) & 0xFF);
        ret += ((int) mDevice.readRegByte(a1) & 0xFF) << 8;
        ret += ((int) mDevice.readRegByte(a2) & 0xFF) << 16;
        if (ret < 8388608) return ret;
        else return ret - 16777216;
    }

    private int readSigned16(int a0, int a1) throws IOException {
        int ret = (mDevice.readRegByte(a0) & 0xFF);
        ret += (mDevice.readRegByte(a1) & 0xFF) << 8;
        if (ret < 32768) return ret;
        else return ret - 65536;
    }

    /**
     * The sensor seems to have an offset to the actual pressure. You can find your local "real"
     * pressure quite easily on the web. Get the measured value from the sensor and compute the
     * difference. The value obtained can be passed to this method to "calibrate" your board's
     * sensor. In the author's case the difference was 6.2 hPa which is quite significant. This
     * error was confirmed with another (unrelated) sensor.
     *
     * @param hPa difference to actual air pressure in hectoPascal (hPa) or millibar.
     * @throws IOException from I2cDevice
     */
    public void setBarometerOffset(double hPa) throws IOException {
        this.mMillibarAdjust = (int) Math.round(hPa * 4096);
    }

    /**
     * Fetch raw value, see the data sheet. <p>Note that this call waits for data to be available.
     * From the data sheet the (selected) refresh rate is 12.5 Hz so the max wait could be
     * 1000/12.5 = 80 milliseconds with an average of 40 milliseconds. Call from asynchronous code
     * if this is an issue. If your code calls this method less frequently then 12.5 times per
     * second there will be no wait.</p>
     *
     * @return The raw sensor value, adjusted by the given offset (if any).
     * @throws IOException from I2cDevice
     */
    public int getBarometerRaw() throws IOException {
        // wait for data available
        while (0 == (mDevice.readRegByte(LPS_STATUS_REG) & 2)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
        return readSigned24(LPS_PRESS_OUT_XL, LPS_PRESS_OUT_L, LPS_PRESS_OUT_H) + mMillibarAdjust;
    }

    /**
     * Fetch raw value, see the data sheet. <p>Note that this call waits for data to be available.
     * From the data sheet the (selected) refresh rate is 12.5 Hz so the max wait could be
     * 1000/12.5 = 80 milliseconds with an average of 40 milliseconds. Call from asynchronous code
     * if this is an issue. If your code calls this method less frequently then 12.5 times per
     * second there will be no wait.</p>
     *
     * @return The raw sensor value.
     * @throws IOException from I2cDevice
     */
    public int getTemperatureRaw() throws IOException {
        // wait for data available
        while (0 == (mDevice.readRegByte(LPS_STATUS_REG) & 1)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
        return readSigned16(LPS_TEMP_OUT_L, LPS_TEMP_OUT_H);
    }

    /**
     * Fetch air pressure in hPa (millibar). <p>Note that this call waits for data to be available.
     * From the data sheet the (selected) refresh rate is 12.5 Hz so the max wait could be
     * 1000/12.5 = 80 milliseconds with an average of 40 milliseconds. Call from asynchronous code
     * if this is an issue. If your code calls this method less frequently then 12.5 times per
     * second there will be no wait.</p>
     *
     * @return The current air pressure in hPa(millibar), adjusted by the given offset (if any).
     * @throws IOException from I2cDevice
     */
    public double getBarometer() throws IOException {
        return getBarometerRaw() / 4096.0;
    }

    /**
     * Fetch the temperature in degrees Celcius. Note that the design of the SenseHat makes this
     * more the temperature of the board then the actual (room) temperature!
     * <p>Also note that this call waits for data to be available.
     * From the data sheet the (selected) refresh rate is 12.5 Hz so the max wait could be
     * 1000/12.5 = 80 milliseconds with an average of 40 milliseconds. Call from asynchronous code
     * if this is an issue. If your code calls this method less frequently then 12.5 times per
     * second there will be no wait.</p>
     *
     * @return The temperature as reported by the sensor in degrees Celcius.
     * @throws IOException from I2cDevice
     */
    public double getTemperature() throws IOException {
        return 42.5 + getTemperatureRaw() / 480.0;
    }
}
