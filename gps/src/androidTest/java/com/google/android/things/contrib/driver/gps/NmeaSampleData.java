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

package com.google.android.things.contrib.driver.gps;

public class NmeaSampleData {

    private static final String BLOCKED_NO_FIX =
            "$GPGGA,,,,,,0,00,99.99,,,,,,*48\r\n" +
            "$GPGSA,A,1,,,,,,,,,,,,,,,*1E\r\n" +
            "$GPGSV,1,1,00*79\r\n" +
            "$GPRMC,,V,,,,,,,,,,N*53\r\n" +
            "$GPVTG,,,,,,,,,N*30\r\n";
    public static byte[] SAMPLE_BLOCKED_NO_FIX = BLOCKED_NO_FIX.getBytes();

    private static final String SAT_VIEW_NO_FIX =
            "$GPRMC,,V,,,,,,,,,,N*53\r\n"+
            "$GPGSA,A,3,02,,,07,,09,24,26,,,,,1.6,1.6,1.0*3D\r\n" +
            "$GPGSV,2,1,07,07,79,048,42,02,51,062,43,26,36,256,42,27,27,138,42*71\r\n" +
            "$GPGSV,2,2,07,09,23,313,42,04,19,159,41,15,12,041,42*41\r\n" +
            "$GPVTG,,T,,M,,N,,K,N*2C\r\n"+
            "$GPGGA,,,,,,0,,,,,,,,*66\r\n";
    public static byte[] SAMPLE_SAT_VIEW_NO_FIX = SAT_VIEW_NO_FIX.getBytes();

    private static final String VALID_FIX =
            "$GPGGA,183730,3907.356,N,12102.482,W,1,05,1.6,646.4,M,-24.1,M,,*75\r\n" +
            "$GPGSA,A,3,02,,,07,,09,24,26,,,,,1.6,1.6,1.0*3D\r\n" +
            "$GPGSV,2,1,07,07,79,048,42,02,51,062,43,26,36,256,42,27,27,138,42*71\r\n" +
            "$GPGSV,2,2,07,09,23,313,42,04,19,159,41,15,12,041,42*41\r\n" +
            "$GPRMC,183729,A,3907.356,N,12102.482,W,000.0,360.0,080301,015.5,E*6F\r\n" +
            "$GPGLL,3907.360,N,12102.481,W,183730,A*33\r\n";
    public static byte[] SAMPLE_VALID_FIX = VALID_FIX.getBytes();

    /*
     * Expected parsed values for the above sample sentences
     */
    public static int EXPECTED_SAT_COUNT = 7;
    public static long EXPECTED_TIMESTAMP = 984076649000L;
    public static double EXPECTED_LATITUDE = 39.1226;
    public static double EXPECTED_LONGITUDE = -121.0413;
}
