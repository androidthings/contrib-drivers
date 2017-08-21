package com.google.android.things.contrib.driver.ws2812b;


import android.annotation.SuppressLint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RunWith(PowerMockRunner.class)
public class BitPatternHolderTest {

    private BitPatternHolder bitPatternHolder;

    @Before
    public void setUp() throws Exception {
        bitPatternHolder = new BitPatternHolder(new BitPatternHolder.Storage() {
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
    }

    @Test
    public void getBitPattern() throws IOException {
        double limit = Math.pow(2, 12);
        for (int i = 0; i < limit; i++) {
            //noinspection ConstantConditions
            if (bitPatternHolder.getBitPattern(i) == null)
            {
                throw new AssertionError("Bit pattern not found for value: " + i);
            }
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void getBitPatternIllegalArgumentException() throws IOException {
        int limit = (int) Math.pow(2, 12);
        bitPatternHolder.getBitPattern(limit);
    }
}
