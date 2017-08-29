package com.google.android.things.contrib.driver.ws2812b;


import android.graphics.Color;
import android.support.annotation.NonNull;

import com.google.android.things.contrib.driver.ws2812b.util.ColorMock;
import com.google.android.things.contrib.driver.ws2812b.util.SimpleColorToBitPatternConverter;
import com.google.android.things.contrib.driver.ws2812b.util.SparseArrayMockCreator;
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
public class Ws2812bTest {
    @Mock
    private SpiDevice mSpiDevice;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    @Test
    public void close() throws IOException {
        Ws2812b ws2812b = createWs2812BDevice();
        ws2812b.close();
        Mockito.verify(mSpiDevice).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Ws2812b ws2812b = createWs2812BDevice();
        ws2812b.close();
        ws2812b.close();
        // Check if the inner SPI device was only closed once
        Mockito.verify(mSpiDevice, Mockito.times(1)).close();
    }

    @Test
    public void writeRed() throws IOException {
        ColorMock.mockStatic();

        int color = Color.RED;

        Ws2812b ws2812b = createWs2812BDevice();
        ws2812b.write(new int [] {Color.RED});

        byte[] bytes = new SimpleColorToBitPatternConverter().constructBitPatterns(color);

        Mockito.verify(mSpiDevice).write(bytes, bytes.length);
    }

    @NonNull
    private Ws2812b createWs2812BDevice() throws IOException {
        TwelveBitIntToBitPatternMapper patternMapper = new TwelveBitIntToBitPatternMapper(SparseArrayMockCreator.createMockedSparseArray());
        ColorToBitPatternConverter converter = new ColorToBitPatternConverter(ColorChannelSequence.RBG, patternMapper);
        return new Ws2812b(mSpiDevice, converter);
    }
}
