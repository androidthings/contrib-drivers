package com.google.android.things.contrib.driver.ws2812b;


import android.annotation.SuppressLint;

import com.google.android.things.contrib.driver.ws2812b.util.BitPatternTo12BitIntConverter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



@RunWith(PowerMockRunner.class)
public class TwelveBitIntToBitPatternMapperTest {

    private TwelveBitIntToBitPatternMapper twelveBitIntToBitPatternMapper = new TwelveBitIntToBitPatternMapper(new TwelveBitIntToBitPatternMapper.Storage() {
            @SuppressLint("UseSparseArrays") // Sparse array is not available for Unit tests
            private Map<Integer, byte []> map = new HashMap<>();
            @Override
            public void put(int key, byte[] value) {
                map.put(key, value);
            }

            @Override
            public byte[] get(int key) {
                return map.get(key);
            }
        });


    @Test
    public void getBitPattern() throws IOException {
        BitPatternTo12BitIntConverter converter = new BitPatternTo12BitIntConverter();
        double limit = Math.pow(2, 12);
        for (int i = 0; i < limit; i++) {
            byte[] bitPattern = twelveBitIntToBitPatternMapper.getBitPattern(i);
            int originalValue = converter.convertBitPatternTo12BitInt(bitPattern);
            Assert.assertEquals(originalValue, i);
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void getBitPatternIllegalArgumentException() throws IOException {
        int limit = (int) Math.pow(2, 12);
        twelveBitIntToBitPatternMapper.getBitPattern(limit);
    }
}
