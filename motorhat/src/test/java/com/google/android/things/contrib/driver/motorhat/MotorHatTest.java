package com.google.android.things.contrib.driver.motorhat;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

public class MotorHatTest {

    @Mock
    I2cDevice mI2c;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    private MotorHat mDriver;

    @Before
    public void setup() throws IOException {
        mDriver = new MotorHat(mI2c);
    }

    @Test
    public void close() throws IOException {
        mDriver.close();
        verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        mDriver.close();
        mDriver.close(); // should not throw
        verify(mI2c, times(1)).close();
    }

    @Test
    public void setMotorSpeed() throws IOException {
        final int speed = 100;
        final int speedCheck = speed << 4;
        mDriver.setMotorSpeed(0, speed);
        verifyPin(8, 0, speedCheck);
        mDriver.setMotorSpeed(1, speed);
        verifyPin(13, 0, speedCheck);
        mDriver.setMotorSpeed(2, speed);
        verifyPin(2, 0, speedCheck);
        mDriver.setMotorSpeed(3, speed);
        verifyPin(7, 0, speedCheck);
    }

    @Test
    public void setMotorSpeed_throwsIfClosed() throws IOException {
        mDriver.close();
        mExpectedException.expect(IllegalStateException.class);
        mDriver.setMotorSpeed(0, 0);
    }

    @Test
    public void setMotorState() throws IOException {
        final int dcOn = 4096, dcOff = 0;
        mDriver.setMotorState(0, MotorHat.MOTOR_STATE_CW);
        verifyPin(9, dcOn, dcOff);
        verifyPin(10, dcOff, dcOn);
        mDriver.setMotorState(1, MotorHat.MOTOR_STATE_CCW);
        verifyPin(12, dcOff, dcOn);
        verifyPin(11, dcOn, dcOff);
        mDriver.setMotorState(2, MotorHat.MOTOR_STATE_RELEASE);
        verifyPin(3, dcOff, dcOn);
        verifyPin(4, dcOff, dcOn);
    }

    @Test
    public void setMotorState_throwsIfClosed() throws IOException {
        mDriver.close();
        mExpectedException.expect(IllegalStateException.class);
        mDriver.setMotorState(0, MotorHat.MOTOR_STATE_RELEASE);
    }

    private void verifyPin(int pin, int on, int off) throws IOException {
        byte onLsb = (byte) (on & 0xFF);
        byte onMsb = (byte) (on >> 8);
        byte offLsb = (byte) (off & 0xFF);
        byte offMsb = (byte) (off >> 8);
        int offset = 4 * pin;
        verify(mI2c).writeRegByte(0x06 + offset, onLsb);
        verify(mI2c).writeRegByte(0x07 + offset, onMsb);
        verify(mI2c).writeRegByte(0x08 + offset, offLsb);
        verify(mI2c).writeRegByte(0x09 + offset, offMsb);
    }
}
