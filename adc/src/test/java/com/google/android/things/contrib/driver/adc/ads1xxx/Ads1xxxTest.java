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

package com.google.android.things.contrib.driver.adc.ads1xxx;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

public class Ads1xxxTest {

    @Test
    public void readSingle_returnsCorrectValue() throws IOException {
        // 12-bit ADC device
        Ads1015Device mockDevice = new Ads1015Device();
        // Default voltage scale is 2.048V
        mockDevice.setChannelValue(2.048f);
        Ads1xxx driver = new Ads1xxx(mockDevice, Ads1xxx.Configuration.ADS1015);

        int result = driver.readSingleEndedInput(0);
        assertEquals(2047, result);
    }

    @Test
    public void readDifferential_returnsNegativeValue() throws IOException {
        // 12-bit ADC device
        Ads1015Device mockDevice = new Ads1015Device();
        mockDevice.setChannelValue(-1f);
        Ads1xxx driver = new Ads1xxx(mockDevice, Ads1xxx.Configuration.ADS1015);

        double result = driver.readDifferentialVoltage(Ads1xxx.INPUT_DIFF_0P_1N);
        assertEquals(-1.0, result, 0.002);
    }

    @Test
    public void readScaled_returnsCorrectValue() throws IOException {
        // 12-bit ADC device
        Ads1015Device mockDevice = new Ads1015Device();
        mockDevice.setChannelValue(3.3f);
        Ads1xxx driver = new Ads1xxx(mockDevice, Ads1xxx.Configuration.ADS1015);

        // Driver will max out at the default scale
        double result = driver.readSingleEndedVoltage(0);
        assertEquals(2.048, result, 0.002);
        // Increase the voltage scale
        driver.setInputRange(Ads1xxx.RANGE_4_096V);
        result = driver.readSingleEndedVoltage(0);
        assertEquals(3.3, result, 0.002);
    }
}
