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

package com.google.android.things.contrib.driver.apa102;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

import android.graphics.Color;

import com.google.android.things.contrib.driver.apa102.Apa102.Mode;
import com.google.android.things.pio.SpiDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(android.graphics.Color.class)
public class Apa102Test {

    @Mock
    SpiDevice mSpiDevice;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void close() throws IOException {
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        leds.close();
        Mockito.verify(mSpiDevice).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        leds.close();
        leds.close(); // should not throw
        Mockito.verify(mSpiDevice, times(1)).close();
    }

    @Test
    public void setBrightness() throws IOException {
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        final int brightness = 10;
        leds.setBrightness(brightness);
        assertEquals(brightness, leds.getBrightness());
    }

    @Test
    public void setBrightness_throwsIfTooSmall() throws IOException {
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        mExpectedException.expect(IllegalArgumentException.class);
        leds.setBrightness(-1);
    }

    @Test
    public void setBrightness_throwsIfTooLarge() throws IOException {
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        mExpectedException.expect(IllegalArgumentException.class);
        leds.setBrightness(Apa102.MAX_BRIGHTNESS + 1);
    }

    @Test
    public void setDirection() throws IOException {
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        leds.setDirection(Apa102.Direction.REVERSED);
        assertEquals(Apa102.Direction.REVERSED, leds.getDirection());
    }

    @Test
    public void setDirection_allowsChangeOfDirection() throws IOException {
        ColorMock.mockStatic();
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        final int brightness = 15;
        leds.setBrightness(brightness);
        final int[] colors = {0xff0000, 0x00ff00, 0x0000ff};
        leds.write(colors);
        int headerSize = 4;
        int resetframeSize = 4;
        int endframeSize = 4;
        Mockito.verify(mSpiDevice).write(Mockito.argThat(BytesMatcher.contains(
                (byte)(0xE0|brightness),
                (byte)(colors[0]&0xff), (byte)(colors[0]>>8&0xff), (byte)(colors[0]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[1]&0xff), (byte)(colors[1]>>8&0xff), (byte)(colors[1]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[2]&0xff), (byte)(colors[2]>>8&0xff), (byte)(colors[2]>>16&0xff)
        )), Mockito.eq(headerSize + colors.length*4 + resetframeSize + endframeSize));

        leds.setDirection(Apa102.Direction.REVERSED);

        leds.write(colors);
        Mockito.verify(mSpiDevice, times(2)).write(Mockito.argThat(BytesMatcher.contains(
                (byte)(0xE0|brightness),
                (byte)(colors[2]&0xff), (byte)(colors[2]>>8&0xff), (byte)(colors[2]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[1]&0xff), (byte)(colors[1]>>8&0xff), (byte)(colors[1]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[0]&0xff), (byte)(colors[0]>>8&0xff), (byte)(colors[0]>>16&0xff)
        )), Mockito.eq(headerSize + colors.length*4 + resetframeSize + endframeSize));
    }

    @Test
    public void write() throws IOException {
        ColorMock.mockStatic();
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        final int brightness = 15;
        leds.setBrightness(brightness);
        final int[] colors = {0xff0000, 0x00ff00, 0x0000ff};
        leds.write(colors);
        int headerSize = 4;
        int resetframeSize = 4;
        int endframeSize = 4;
        Mockito.verify(mSpiDevice).write(Mockito.argThat(BytesMatcher.contains(
                (byte)(0xE0|brightness),
                (byte)(colors[0]&0xff), (byte)(colors[0]>>8&0xff), (byte)(colors[0]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[1]&0xff), (byte)(colors[1]>>8&0xff), (byte)(colors[1]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[2]&0xff), (byte)(colors[2]>>8&0xff), (byte)(colors[2]>>16&0xff)
        )), Mockito.eq(headerSize + colors.length*4 + resetframeSize + endframeSize));
    }

    @Test
    public void write_reversed() throws IOException {
        ColorMock.mockStatic();
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.REVERSED);
        final int brightness = 15;
        leds.setBrightness(brightness);
        final int[] colors = {0xff0000, 0x00ff00, 0x0000ff};
        leds.write(colors);
        int headerSize = 4;
        int resetframeSize = 4;
        int endframeSize = 4;
        Mockito.verify(mSpiDevice).write(Mockito.argThat(BytesMatcher.contains(
                (byte)(0xE0|brightness),
                (byte)(colors[2]&0xff), (byte)(colors[2]>>8&0xff), (byte)(colors[2]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[1]&0xff), (byte)(colors[1]>>8&0xff), (byte)(colors[1]>>16&0xff),
                (byte)(0xE0|brightness),
                (byte)(colors[0]&0xff), (byte)(colors[0]>>8&0xff), (byte)(colors[0]>>16&0xff)
        )), Mockito.eq(headerSize + colors.length*4 + resetframeSize + endframeSize));
    }

    @Test
    public void write_throwsIfClosed() throws IOException {
        Apa102 leds = new Apa102(mSpiDevice, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
        leds.close();
        mExpectedException.expect(IllegalStateException.class);
        leds.write(new int[] {0xff0000, 0x00ff00, 0x0000ff});
    }

    @Test
    public void copyApaColorData() {
        ColorMock.mockStatic();

        final byte brightness = 15;
        final byte r = (byte) 0x33;
        final byte g = (byte) 0xB5;
        final byte b = (byte) 0xE5;
        // #HOLOYOLO
        final int color = Color.argb(0xFF, r & 0xFF, g & 0xFF, b & 0xFF); // suppress sign extension

        final byte[] dest = new byte[4];
        Apa102.copyApaColorData(brightness, color, Mode.BGR, dest, 0);
        assertArrayEquals(new byte[]{brightness, b, g, r}, dest);
        Apa102.copyApaColorData(brightness, color, Mode.BRG, dest, 0);
        assertArrayEquals(new byte[]{brightness, b, r, g}, dest);
        Apa102.copyApaColorData(brightness, color, Mode.GBR, dest, 0);
        assertArrayEquals(new byte[]{brightness, g, b, r}, dest);
        Apa102.copyApaColorData(brightness, color, Mode.GRB, dest, 0);
        assertArrayEquals(new byte[]{brightness, g, r, b}, dest);
        Apa102.copyApaColorData(brightness, color, Mode.RBG, dest, 0);
        assertArrayEquals(new byte[]{brightness, r, b, g}, dest);
        Apa102.copyApaColorData(brightness, color, Mode.RGB, dest, 0);
        assertArrayEquals(new byte[]{brightness, r, g, b}, dest);
    }

    @Test
    public void copyApaColorData_throwsIfNullArray() {
        mExpectedException.expect(IllegalArgumentException.class);
        Apa102.copyApaColorData((byte) 0, 0, Mode.BGR, null, 0);
    }

    @Test
    public void copyApaColorData_throwsIfArrayTooSmall() {
        mExpectedException.expect(IllegalArgumentException.class);
        Apa102.copyApaColorData((byte) 0, 0, Mode.BGR, new byte[3], 0);
    }
}
