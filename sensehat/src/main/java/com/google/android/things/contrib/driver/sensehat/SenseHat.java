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

package com.google.android.things.contrib.driver.sensehat;

import java.io.IOException;

/**
 * Driver factory for the Sense Hat.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SenseHat {
    public static final int I2C_ADDRESS =  0x46;
    public static final String BUS_DISPLAY = "I2C1";
    public static final int DISPLAY_WIDTH = LedMatrix.WIDTH;
    public static final int DISPLAY_HEIGHT = LedMatrix.HEIGHT;

    public static LedMatrix openDisplay() throws IOException {
        return new LedMatrix(BUS_DISPLAY);
    }
}
