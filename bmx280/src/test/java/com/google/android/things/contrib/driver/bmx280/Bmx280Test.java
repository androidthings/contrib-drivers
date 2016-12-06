/*
 * Copyright 2016 Google Inc.
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

package com.google.android.things.contrib.driver.bmx280;

import junit.framework.Assert;

import org.junit.Test;

public class Bmx280Test {

    // from the BMP280 datasheet.
    // https://ae-bst.resource.bosch.com/media/_tech/media/datasheets/BST-BMP280-DS001-12.pdf
    private static final int[] TEMP_CALIBRATION = {27504, 26435, -1000};
    private static final int[] PRESSURE_CALIBRATION = {36477, -10685, 3024, 2855, 140, -7, 15500,
            -14600, 6000};
    private static final int RAW_TEMPERATURE = 519888;
    private static final int RAW_PRESSURE = 415148;

    private static final float EXPECTED_TEMPERATURE = 25.08f;
    private static final float EXPECTED_FINE_TEMPERATURE = 128422.0f;
    private static final float EXPECTED_PRESSURE = 1006.5327f;
    // Note: datasheet points out that the calculated values can differ slightly because of
    // rounding. We'll check that the results are within a tolerance of 0.1%
    private static final float TOLERANCE = .001f;

    @Test
    public void testCompensateTemperature() {
        final float[] tempResults = Bmx280.compensateTemperature(RAW_TEMPERATURE, TEMP_CALIBRATION);
        Assert.assertEquals(tempResults[0], EXPECTED_TEMPERATURE, EXPECTED_TEMPERATURE * TOLERANCE);
        Assert.assertEquals(tempResults[1], EXPECTED_FINE_TEMPERATURE,
                EXPECTED_FINE_TEMPERATURE * TOLERANCE);
    }

    @Test
    public void testCompensatePressure() {
        final float[] tempResults = Bmx280.compensateTemperature(RAW_TEMPERATURE, TEMP_CALIBRATION);
        final float pressure = Bmx280.compensatePressure(RAW_PRESSURE, tempResults[1],
                PRESSURE_CALIBRATION);
        Assert.assertEquals(pressure, EXPECTED_PRESSURE, EXPECTED_PRESSURE * TOLERANCE);
    }
}
