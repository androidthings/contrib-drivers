/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.htu21d;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import com.google.android.things.pio.I2cDevice;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

public class Htu21dTest {

    // Sensor readings and expected results for testing compensation functions
    private static final int RAW_TEMPERATURE = 28671;
    private static final int RAW_HUMIDITY = 26662;

    private static final float EXPECTED_TEMPERATURE = 30.0248f;
    private static final float EXPECTED_HUMIDITY = 44.8537f;
    private static final float TOLERANCE = .001f;

    @Mock
    private I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void testCRCCalculation() {
        long crc = Htu21d.calculateCRC8(new byte[]{0x68, 0x3A});
        Assert.assertEquals(crc, 0x7C);
        crc = Htu21d.calculateCRC8(new byte[]{0x4E, (byte) 0x85});
        Assert.assertEquals(crc, 0x6B);
    }

    @Test
    public void testCompensateTemperature() {
        final float tempResult = Htu21d.compensateTemperature(RAW_TEMPERATURE);
        Assert.assertEquals(tempResult, EXPECTED_TEMPERATURE, EXPECTED_TEMPERATURE * TOLERANCE);
    }

    @Test
    public void testCompensateHumidity() {
        final float humResult = Htu21d.compensateHumidity(RAW_HUMIDITY);
        Assert.assertEquals(humResult, EXPECTED_HUMIDITY, EXPECTED_HUMIDITY * TOLERANCE);
    }

    @Test
    public void close() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        htu21d.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }

    @Test
    public void readTemperature() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readTemperature();
        Mockito.verify(mI2c).readRegBuffer(eq(0xE3), any(byte[].class), eq(3));
    }

    @Test
    public void readTemperature_withoutHold() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readTemperature(false);
        Mockito.verify(mI2c).write(eq(new byte[]{(byte)0xF3}), eq(1));
        Mockito.verify(mI2c).read(any(byte[].class), eq(2));
    }

    @Test
    public void readTemperature_throwsIfClosed() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        htu21d.readTemperature();
    }

    @Test
    public void readHumidity() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readHumidity();
        Mockito.verify(mI2c).readRegBuffer(eq(0xE5), any(byte[].class), eq(3));
    }

    @Test
    public void readHumidity_withoutHold() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readHumidity(false);
        Mockito.verify(mI2c).write(eq(new byte[]{(byte)0xF5}), eq(1));
        Mockito.verify(mI2c).read(any(byte[].class), eq(2));
    }

    @Test
    public void readHumidity_throwsIfClosed() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        htu21d.readHumidity();
    }

    @Test
    public void readTemperatureAndHumidity() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readTemperatureAndHumidity();
        Mockito.verify(mI2c).readRegBuffer(eq(0xE3), any(byte[].class), eq(3));
        Mockito.verify(mI2c).readRegBuffer(eq(0xE5), any(byte[].class), eq(3));
    }

    @Test
    public void readTemperatureAndHumidity_noHold() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.readTemperatureAndHumidity(false);
        Mockito.verify(mI2c).readRegBuffer(eq(0xF3), any(byte[].class), eq(3));
        Mockito.verify(mI2c).readRegBuffer(eq(0xF5), any(byte[].class), eq(3));
    }

    @Test
    public void readTemperatureAndHumidity_throwsIfClosed() throws IOException {
        Htu21d htu21d = new Htu21d(mI2c);
        htu21d.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("not open");
        htu21d.readTemperatureAndHumidity();
    }
}
