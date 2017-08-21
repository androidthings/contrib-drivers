package com.google.android.things.contrib.driver.ws2812b;


import android.graphics.Color;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


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
        Ws2812b ws2812b = new Ws2812b(mSpiDevice, Ws2812b.RGB);
        ws2812b.close();
        Mockito.verify(mSpiDevice).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        Ws2812b ws2812b = new Ws2812b(mSpiDevice, Ws2812b.RBG);
        ws2812b.close();
        ws2812b.close();
        // Check if the inner SPI device was only closed once
        Mockito.verify(mSpiDevice, Mockito.times(1)).close();
    }

    @Test
    public void writeRed() throws IOException {
        ColorMock.mockStatic();

        int color = Color.RED;

        Ws2812b ws2812b = new Ws2812b(mSpiDevice, Ws2812b.RBG);
        ws2812b.write(new int [] {Color.RED});

        List<Boolean> bitPatterns = new ArrayList<>();
        List<Boolean> oneBitPattern = Arrays.asList(true, true, false);
        List<Boolean> zeroBitPattern = Arrays.asList(true, false, false);


        int highestBit = 1<<23;

        for (int i = 0; i < 24; i++) {
            List<Boolean> bitPattern = (color & highestBit) == highestBit ?  oneBitPattern : zeroBitPattern;
            bitPatterns.addAll(bitPattern);
            color = color << 1;
        }

        Iterator<Boolean> iterator = bitPatterns.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            iterator.next();
            if (i == 8)
            {
                iterator.remove();
                i = 0;
                continue;
            }
            i++;
        }


        int [] masks = {
                0b1000_0000,
                0b0100_0000,
                0b0010_0000,
                0b0001_0000,
                0b0000_1000,
                0b0000_0100,
                0b0000_0010,
                0b0000_0001,
        };

        byte [] bytes = new byte[bitPatterns.size() / 8];
        byte currentByte = 0;
        i = 0;
        int j = 0;
        for (Boolean bitValue : bitPatterns) {
            if (bitValue)
            {
                currentByte |= masks[i];
            }
            if (i == 7)
            {
                bytes[j++] = currentByte;
                currentByte = 0;
                i = 0;
                continue;
            }
            i++;
        }


        int firstTwelveBit = 0x00FFF000;
        int secondTwelveBit = 0x00000FFF;
        int firstValue = (Color.RED & firstTwelveBit) >> 12;
        int secondValue = Color.RED & secondTwelveBit;
        BitPatternHolder bitPatternHolder = new BitPatternHolder();
        byte[] bitPattern = bitPatternHolder.getBitPattern(firstValue);
        byte[] secondBitPattern = bitPatternHolder.getBitPattern(secondValue);


        Mockito.verify(mSpiDevice).write(bytes, bytes.length);
    }
}
