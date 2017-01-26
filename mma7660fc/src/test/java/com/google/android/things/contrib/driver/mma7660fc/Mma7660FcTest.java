/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.things.contrib.driver.mma7660fc;

import com.google.android.things.pio.I2cDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

public class Mma7660FcTest {

    @Mock
    I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void close() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.close();
        driver.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }

    @Test
    public void setMode() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.setMode(Mma7660Fc.MODE_ACTIVE);
        Mockito.verify(mI2c).writeRegByte(0x07, (byte) Mma7660Fc.MODE_ACTIVE);

        Mockito.reset(mI2c);

        driver.setMode(Mma7660Fc.MODE_STANDBY);
        Mockito.verify(mI2c).writeRegByte(0x07, (byte) Mma7660Fc.MODE_STANDBY);
    }

    @Test
    public void setMode_throwsIfClosed() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.setMode(Mma7660Fc.MODE_ACTIVE);
    }

    @Test
    public void getMode() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.getMode();
        Mockito.verify(mI2c).readRegByte(0x07);
    }

    @Test
    public void getMode_throwsIfClosed() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.getMode();
    }

    @Test
    public void setSamplingRate() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.setSamplingRate(Mma7660Fc.RATE_1HZ);
        Mockito.verify(mI2c).writeRegByte(0x08, (byte) Mma7660Fc.RATE_1HZ);
    }

    @Test
    public void setSamplingRate_throwsIfClosed() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.setSamplingRate(Mma7660Fc.RATE_1HZ);
    }

    @Test
    public void getSamplingRate() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.getSamplingRate();
        Mockito.verify(mI2c).readRegByte(0x08);
    }

    @Test
    public void getSamplingRate_throwsIfClosed() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.getSamplingRate();
    }

    @Test
    public void readSample() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.readSample();
        Mockito.verify(mI2c).read(aryEq(new byte[3]), eq(3));
    }

    @Test
    public void readSample_throwsIfClosed() throws IOException {
        Mma7660Fc driver = new Mma7660Fc(mI2c);
        driver.close();
        mExpectedException.expect(IllegalStateException.class);
        driver.readSample();
    }
}
