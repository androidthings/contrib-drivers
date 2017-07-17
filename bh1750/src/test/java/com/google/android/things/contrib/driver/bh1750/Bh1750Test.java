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

package com.google.android.things.contrib.driver.bh1750;

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

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

public class Bh1750Test {

    @Mock
    private I2cDevice mI2c;

    @Rule
    public MockitoRule mMokitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void close() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        bh1750.close();
        Mockito.verify(mI2c).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        bh1750.close();
        bh1750.close(); // should not throw
        Mockito.verify(mI2c, times(1)).close();
    }

    @Test
    public void initialization() throws IOException {
        new Bh1750(mI2c);
        Mockito.verify(mI2c).write(aryEq(new byte[]{Bh1750.POWER_ON}), eq(1));
        Mockito.verify(mI2c).write(aryEq(new byte[]{Bh1750.CONTINUOUS_HIGH_RES_MODE}), eq(1));
        Mockito.verify(mI2c).write(aryEq(new byte[]{Bh1750.RESET}), eq(1));
    }

    @Test
    public void setMode() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        Mockito.reset(mI2c);

        bh1750.setMode(Bh1750.POWER_ON);
        Mockito.verify(mI2c).write(aryEq(new byte[]{Bh1750.POWER_ON}), eq(1));

        Mockito.reset(mI2c);

        bh1750.setMode(Bh1750.POWER_DOWN);
        Mockito.verify(mI2c).write(aryEq(new byte[]{Bh1750.POWER_DOWN}), eq(1));
    }

    @Test
    public void setMode_throwsIfClosed() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        bh1750.close();
        mExpectedException.expect(IllegalStateException.class);
        bh1750.setMode(Bh1750.POWER_ON);
    }

    @Test
    public void setResolution() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        Mockito.reset(mI2c);

        bh1750.setResolution(Bh1750.ONE_TIME_HIGH_RES_MODE);
        Mockito.verify(mI2c).write(aryEq(new byte[]{Bh1750.ONE_TIME_HIGH_RES_MODE}), eq(1));}

    @Test
    public void setResolution_throwsIfClosed() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        bh1750.close();
        mExpectedException.expect(IllegalStateException.class);
        bh1750.setResolution(Bh1750.ONE_TIME_HIGH_RES_MODE);
    }

    @Test
    public void readLightLevel() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        bh1750.readLightLevel();

        Mockito.verify(mI2c).read(any(byte[].class), eq(2));
    }

    @Test
    public void readLightLevel_throwsIfClosed() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        bh1750.close();

        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("I2C device not open");
        bh1750.readLightLevel();
    }

    @Test
    public void convertRawLightToLux() throws IOException {
        Bh1750 bh1750 = new Bh1750(mI2c);
        Assert.assertEquals(100.0f, bh1750.convertRawValueToLux(120), 0.01);
    }
}
