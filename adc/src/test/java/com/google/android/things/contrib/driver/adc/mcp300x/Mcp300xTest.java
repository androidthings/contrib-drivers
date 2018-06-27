/*
 * Copyright 2018 Google Inc.
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

package com.google.android.things.contrib.driver.adc.mcp300x;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

public class Mcp300xTest {

    @Test
    public void readSingle_returnsCorrectValue() throws IOException {
        Mcp3008Device mockDevice = new Mcp3008Device();
        mockDevice.setChannelValues(0, 256, 512, 1023);
        Mcp300x driver = new Mcp300x(mockDevice, Mcp300x.Configuration.MCP3008);

        int result = driver.readSingleEndedInput(0);
        assertEquals("Channel 0 should return 0", 0, result);
        result = driver.readSingleEndedInput(1);
        assertEquals("Channel 1 should return 256", 256, result);
        result = driver.readSingleEndedInput(2);
        assertEquals("Channel 2 should return 512", 512, result);
        result = driver.readSingleEndedInput(3);
        assertEquals("Channel 3 should return 1023", 1023, result);
    }

    @Test
    public void readDifferential_returnsCorrectValue() throws IOException {
        Mcp3008Device mockDevice = new Mcp3008Device();
        mockDevice.setChannelValues(512, 256);
        Mcp300x driver = new Mcp300x(mockDevice, Mcp300x.Configuration.MCP3008);

        int result = driver.readDifferentialInput(Mcp300x.MODE_DIFF_0P_1N);
        assertEquals("Normal differential should return 256", 256, result);
        result = driver.readDifferentialInput(Mcp300x.MODE_DIFF_1P_0N);
        assertEquals("Inverted differential should return 0", 0, result);
    }
}
