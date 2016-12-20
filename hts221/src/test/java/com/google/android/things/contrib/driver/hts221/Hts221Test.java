/*
 * Copyright 2016 Macro Yau
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

package com.google.android.things.contrib.driver.hts221;

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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.byteThat;
import static org.mockito.Matchers.eq;

public class Hts221Test {

    @Mock
    I2cDevice mI2c;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void testCompensateHumidity() {
        // Example from the datasheet
        // http://www.st.com/resource/en/datasheet/hts221.pdf
        int h0T0Out = (short) 0x4000;
        int h1T0Out = (short) 0x6000;
        int hOut = (short) 0x5000;
        int h0 = 40;
        int h1 = 80;

        float[] calibration = Hts221.calibrateHumidityParameters(h0, h1, h0T0Out, h1T0Out);
        Assert.assertEquals(30.0f, Hts221.compensateSample(hOut, calibration));
    }

    @Test
    public void testCompensateTemperature() {
        // Example from the datasheet
        // http://www.st.com/resource/en/datasheet/hts221.pdf
        int t0Out = (short) 300;
        int t1Out = (short) 500;
        int tOut = (short) 400;
        int t0 = 80;
        int t1 = 160;

        float[] calibration = Hts221.calibrateTemperatureParameters(t0, t1, t0Out, t1Out);
        Assert.assertEquals(15.0f, Hts221.compensateSample(tOut, calibration));
    }

    @Test
    public void close() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        hts221.close(); // Should not throw
    }

    @Test
    public void setMode() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);

        Mockito.reset(mI2c);

        hts221.setMode(Hts221.MODE_ACTIVE);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x80)));
    }

    @Test
    public void setMode_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        hts221.setMode(Hts221.MODE_ACTIVE);
    }

    @Test
    public void setBlockDataUpdate() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);

        // Disable BDU
        hts221.setBlockDataUpdate(false);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x00)));

        Mockito.reset(mI2c);

        // Enable BDU
        hts221.setBlockDataUpdate(true);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x04)));
    }

    @Test
    public void setBlockDataUpdate_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        hts221.setBlockDataUpdate(true);
    }

    @Test
    public void setOutputDataRate() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);

        Mockito.reset(mI2c);

        // One-shot
        hts221.setOutputDataRate(Hts221.HTS221_ODR_ONE_SHOT);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x00)));

        Mockito.reset(mI2c);

        // 1 Hz
        hts221.setOutputDataRate(Hts221.HTS221_ODR_1_HZ);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x01)));

        Mockito.reset(mI2c);

        // 7 Hz
        hts221.setOutputDataRate(Hts221.HTS221_ODR_7_HZ);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x02)));

        Mockito.reset(mI2c);

        // 12.5 Hz
        hts221.setOutputDataRate(Hts221.HTS221_ODR_12_5_HZ);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x03)));
    }

    @Test
    public void setOutputDataRate_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        hts221.setOutputDataRate(Hts221.HTS221_ODR_12_5_HZ);
    }

    @Test
    public void setAveragedSamples() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);

        // AVGH_64 + AVGT_2
        hts221.setAveragedSamples(Hts221.AV_CONF_AVGH_64, Hts221.AV_CONF_AVGT_2);
        Mockito.verify(mI2c).writeRegByte(eq(0x10), byteThat(new BitsMatcher((byte) 0x04)));

        Mockito.reset(mI2c);

        // AVGH_512 + AVGT_256
        hts221.setAveragedSamples(Hts221.AV_CONF_AVGH_512, Hts221.AV_CONF_AVGT_256);
        Mockito.verify(mI2c).writeRegByte(eq(0x10), byteThat(new BitsMatcher((byte) 0x3F)));
    }

    @Test
    public void setAveragedSamples_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        hts221.setAveragedSamples(Hts221.AV_CONF_AVGH_512, Hts221.AV_CONF_AVGT_256);
    }

    @Test
    public void readHumidity() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x02);
        hts221.readHumidity();
        Mockito.verify(mI2c).readRegBuffer(eq(0x28 | 0x80), any(byte[].class), eq(2));
    }

    @Test
    public void readHumidity_throwsIfDataNotYetAvailable() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x00);
        mExpectedException.expect(IOException.class);
        mExpectedException.expectMessage("Humidity data is not yet available");
        hts221.readHumidity();
    }

    @Test
    public void readHumidity_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        hts221.readHumidity();
    }

    @Test
    public void readTemperature() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x01);
        hts221.readTemperature();
        Mockito.verify(mI2c).readRegBuffer(eq(0x2A | 0x80), any(byte[].class), eq(2));
    }

    @Test
    public void readTemperature_throwsIfDataNotYetAvailable() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x00);
        mExpectedException.expect(IOException.class);
        mExpectedException.expectMessage("Temperature data is not yet available");
        hts221.readTemperature();
    }

    @Test
    public void readTemperature_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBC);
        Hts221 hts221 = new Hts221(mI2c);
        hts221.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        hts221.readTemperature();
    }
    
}
