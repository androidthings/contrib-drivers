package com.google.android.things.contrib.driver.sensehat;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.Closeable;
import java.io.IOException;

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
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * <p>
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
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
        int ret = mDevice.readRegByte(a0);
        ret |= ((int) mDevice.readRegByte(a1)) << 8;
        ret |= ((int) mDevice.readRegByte(a2)) << 16;
        if (ret < 8388608) return ret;
        else return ret - 16777216;
    }

    private int readSigned16(int a0, int a1) throws IOException {
        int ret = (mDevice.readRegByte(a0) & 0xFF);
        ret |= (mDevice.readRegByte(a1) & 0xFF) << 8;
        if (ret < 32768) return ret;
        else return ret - 65536;
    }

    public int getBarometerRaw() throws IOException {
        return readSigned24(LPS_PRESS_OUT_XL, LPS_PRESS_OUT_L, LPS_PRESS_OUT_H);
    }

    public int getTemperatureRaw() throws IOException {
        return readSigned16(LPS_TEMP_OUT_L, LPS_TEMP_OUT_H);
    }
}
