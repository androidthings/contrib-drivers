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

package com.google.android.things.contrib.driver.ws2801;

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
public class Ws2801Test {

    @Mock
    SpiDevice mSpiDevice;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void write() throws IOException {
        ColorMock.mockStatic();
        Ws2801 leds = new Ws2801(mSpiDevice, Ws2801.Mode.BGR, Ws2801.Direction.NORMAL);
        final int[] colors = {0xff00ff, 0x00ff00, 0x0000ff};
        leds.write(colors);
        Mockito.verify(mSpiDevice).write(Mockito.argThat(BytesMatcher.contains(
                (byte)(colors[0]&0xff), (byte)(colors[0]>>8&0xff), (byte)(colors[0]>>16&0xff),
                (byte)(colors[1]&0xff), (byte)(colors[1]>>8&0xff), (byte)(colors[1]>>16&0xff),
                (byte)(colors[2]&0xff), (byte)(colors[2]>>8&0xff), (byte)(colors[2]>>16&0xff)
        )), Mockito.eq(colors.length*3));
    }

    @Test
    public void write_reversed() throws IOException {
        ColorMock.mockStatic();
        Ws2801 leds = new Ws2801(mSpiDevice, Ws2801.Mode.BGR, Ws2801.Direction.REVERSED);
        final int[] colors = {0xff0000, 0x00ff00, 0x0000ff};
        leds.write(colors);
        Mockito.verify(mSpiDevice).write(Mockito.argThat(BytesMatcher.contains(
                (byte)(colors[2]&0xff), (byte)(colors[2]>>8&0xff), (byte)(colors[2]>>16&0xff),
                (byte)(colors[1]&0xff), (byte)(colors[1]>>8&0xff), (byte)(colors[1]>>16&0xff),
                (byte)(colors[0]&0xff), (byte)(colors[0]>>8&0xff), (byte)(colors[0]>>16&0xff)
        )), Mockito.eq(colors.length*3));
    }
}
