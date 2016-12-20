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

package com.google.android.things.contrib.driver.lps25h;

import com.google.android.things.pio.I2cDevice;

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

public class Lps25hTest {

    @Mock
    I2cDevice mI2c;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void close() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        lps25h.close(); // Should not throw
    }

    @Test
    public void setMode() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);

        Mockito.reset(mI2c);

        lps25h.setMode(Lps25h.MODE_ACTIVE);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x80)));
    }

    @Test
    public void setMode_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        lps25h.setMode(Lps25h.MODE_ACTIVE);
    }

    @Test
    public void setBlockDataUpdate() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);

        // Disable BDU
        lps25h.setBlockDataUpdate(false);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x00)));

        Mockito.reset(mI2c);

        // Enable BDU
        lps25h.setBlockDataUpdate(true);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) 0x04)));
    }

    @Test
    public void setBlockDataUpdate_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        lps25h.setBlockDataUpdate(true);
    }

    @Test
    public void setOutputDataRate() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);

        Mockito.reset(mI2c);

        // One-shot
        lps25h.setOutputDataRate(Lps25h.LPS25H_ODR_ONE_SHOT);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) (0x00 << 4))));

        Mockito.reset(mI2c);

        // 1 Hz
        lps25h.setOutputDataRate(Lps25h.LPS25H_ODR_1_HZ);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) (0x01 << 4))));

        Mockito.reset(mI2c);

        // 7 Hz
        lps25h.setOutputDataRate(Lps25h.LPS25H_ODR_7_HZ);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) (0x02 << 4))));

        Mockito.reset(mI2c);

        // 12.5 Hz
        lps25h.setOutputDataRate(Lps25h.LPS25H_ODR_12_5_HZ);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) (0x03 << 4))));

        Mockito.reset(mI2c);

        // 25 Hz
        lps25h.setOutputDataRate(Lps25h.LPS25H_ODR_25_HZ);
        Mockito.verify(mI2c).writeRegByte(eq(0x20), byteThat(new BitsMatcher((byte) (0x04 << 4))));
    }

    @Test
    public void setOutputDataRate_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        lps25h.setOutputDataRate(Lps25h.LPS25H_ODR_25_HZ);
    }

    @Test
    public void setAveragedSamples() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);

        Mockito.reset(mI2c);

        // AVGP_32 + AVGT_16
        lps25h.setAveragedSamples(Lps25h.RES_CONF_AVGP_32, Lps25h.RES_CONF_AVGT_16);
        Mockito.verify(mI2c).writeRegByte(eq(0x10), byteThat(new BitsMatcher((byte) 0x05)));

        Mockito.reset(mI2c);

        // AVGP_512 + AVGT_64
        lps25h.setAveragedSamples(Lps25h.RES_CONF_AVGP_512, Lps25h.RES_CONF_AVGT_64);
        Mockito.verify(mI2c).writeRegByte(eq(0x10), byteThat(new BitsMatcher((byte) 0x08)));
    }

    @Test
    public void setAveragedSamples_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        lps25h.setAveragedSamples(Lps25h.RES_CONF_AVGP_512, Lps25h.RES_CONF_AVGT_64);
    }

    @Test
    public void readPressure() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x02);
        lps25h.readPressure();
        Mockito.verify(mI2c).readRegBuffer(eq(0x28 | 0x80), any(byte[].class), eq(3));
    }

    @Test
    public void readPressure_throwsIfDataNotYetAvailable() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x00);
        mExpectedException.expect(IOException.class);
        mExpectedException.expectMessage("Pressure data is not yet available");
        lps25h.readPressure();
    }

    @Test
    public void readPressure_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        lps25h.readPressure();
    }

    @Test
    public void readTemperature() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x01);
        lps25h.readTemperature();
        Mockito.verify(mI2c).readRegBuffer(eq(0x2B | 0x80), any(byte[].class), eq(2));
    }

    @Test
    public void readTemperature_throwsIfDataNotYetAvailable() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        Mockito.when(mI2c.readRegByte(0x27)).thenReturn((byte) 0x00);
        mExpectedException.expect(IOException.class);
        mExpectedException.expectMessage("Temperature data is not yet available");
        lps25h.readTemperature();
    }

    @Test
    public void readTemperature_throwsIfClosed() throws IOException {
        Mockito.when(mI2c.readRegByte(0x0F)).thenReturn((byte) 0xBD);
        Lps25h lps25h = new Lps25h(mI2c);
        lps25h.close();
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device is already closed");
        lps25h.readTemperature();
    }
    
}
