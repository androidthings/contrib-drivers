package com.google.brillo.driver.bmx280;

import android.hardware.pio.I2cDevice;
import android.hardware.pio.PeripheralManagerService;
import android.hardware.userdriver.sensors.TemperatureSensorDriver;
import android.system.ErrnoException;

import java.io.Closeable;

/**
 * Driver for the BMP/BME 280 temperature sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Bmx280 implements Closeable {
    private static final String TAG = Bmx280.class.getSimpleName();
    /**
     * I2C address for the the sensor.
     */
    public static final int I2C_ADDRESS = 0x77;
    /**
     * Sensor constants from the datasheet.
     * https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
     */
    /**
     * Mininum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40.f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 85.f;
    /**
     * Maximum power consumption when measure temperature in micro ampere.
     */
    public static final float MAX_TEMP_POWER_UA = 325.f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 181.f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 23.1f;

    private static final int BMP280_REG_ID = 0xD0;
    private static final int BMP280_REG_CTRL = 0xF4;
    private static final int BMP280_REG_TEMP = 0xFA;
    private static final int BMP280_REG_TEMP_CALIB_1 = 0x88;
    private static final int BMP280_REG_TEMP_CALIB_2 = 0x8A;
    private static final int BMP280_REG_TEMP_CALIB_3 = 0x8C;
    private static final int BMP280_POWER_MODE_NORMAL = 0b00000011;
    private static final int BMP280_OVERSAMPLING_PRESSURE_SKIP = 0b00000000;
    private static final int BMP280_OVERSAMPLING_TEMP_1X = 0b00100000;

    private I2cDevice mDevice;
    private int mChipId;
    private int[] mCalibrationData = new int[3];

    /**
     * Create a new BMP/BME280 sensor driver connected on the given bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws ErrnoException
     */
    public Bmx280(String bus) throws ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
        connect(device);
    }
    /**
     * Create a new BMP/BME280 sensor driver connected to the given I2c device.
     * @param device I2C device of the sensor.
     * @throws ErrnoException
     */
    public Bmx280(I2cDevice device) throws ErrnoException {
        connect(device);
    }

    private void connect(I2cDevice device) throws ErrnoException {
        mDevice = device;
        mChipId = mDevice.readRegByte(BMP280_REG_ID);
        // Read unsigned short temp calibration data 1.
        mCalibrationData[0] = mDevice.readRegWord(BMP280_REG_TEMP_CALIB_1);
        // Read signed short temp calibration data 2.
        mCalibrationData[1] = (short) mDevice.readRegWord(BMP280_REG_TEMP_CALIB_2);
        // Read signed short temp calibration data 3.
        mCalibrationData[2] = (short) mDevice.readRegWord(BMP280_REG_TEMP_CALIB_3);
        // Configure Sensor temperature reading.
        // Power mode: Normal
        // Temperature resolution: 16bit
        // Pressure resolution: skip
        mDevice.writeRegByte(BMP280_REG_CTRL, (byte)(BMP280_POWER_MODE_NORMAL
                | BMP280_OVERSAMPLING_TEMP_1X
                | BMP280_OVERSAMPLING_PRESSURE_SKIP));
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }
        mDevice.close();
        mDevice = null;
    }

    /**
     * Returns the sensor chip ID.
     * @return
     */
    public int getChipId() {
        return mChipId;
    }


    private byte[] mBuffer = new byte[3];
    /**
     * Read a temperate sample (20bit precision).
     * @return
     */
    public float readTemperature() throws IllegalStateException, ErrnoException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is closed");
        }
        mDevice.readRegBuffer(BMP280_REG_TEMP, mBuffer, 3);
        // msb[7:0] lsb[7:0] xlsb[7:4]
        int msb = mBuffer[0] & 0xff;
        int lsb = mBuffer[1] & 0xff;
        int xlsb = mBuffer[2] & 0xf0;
        // Convert to 20bit integer
        int rawTemp = (msb << 16 | lsb << 8 | xlsb) >> 4;
        // Compensate temperature using calibration data.
        return compensateTemperature(rawTemp, mCalibrationData);
    }

    // Compensation formula from the BMP280 datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    private static float compensateTemperature(int rawTemp, int[] calibrationData) {
        // Compensate temperature according to calibration data in the datasheet example.
        float adc_T = rawTemp;
        int dig_T1 = calibrationData[0];
        int dig_T2 = calibrationData[1];
        int dig_T3 = calibrationData[2];
        float var1 = (adc_T / 16384.0f - dig_T1 / 1024.0f) * dig_T2;
        float var2 = ((adc_T / 131072.0f - dig_T1 / 8192.0f) *
                (adc_T / 131072.0f - dig_T1 / 8192.0f)) * dig_T3;
        return (var1 + var2) / 5120.0f;
    }

    /**
     * Create a new framework temperature sensor driver.
     * @return the temperature sensor driver.
     */
    public TemperatureSensorDriver createTemperatureSensorDriver() {
        return Bmx280TemperatureSensorDriver.build(this);
    }
}
