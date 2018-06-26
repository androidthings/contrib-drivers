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

import android.location.GnssStatus;
import android.location.Location;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@RunWith(AndroidJUnit4.class)
public class NmeaTest {

    private static final int DEFAULT_BAUD = 9600;
    private static final float DEFAULT_ACCURACY = 1.5f;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Test
    public void testGps_Blocked_NoFix() throws IOException {
        // Set up fake device
        LoopbackUartDevice gpsDevice = new LoopbackUartDevice();
        GpsModuleCallback mockCallback = Mockito.mock(GpsModuleCallback.class);

        NmeaGpsModule gpsModule = new NmeaGpsModule(gpsDevice, DEFAULT_BAUD, DEFAULT_ACCURACY, null);
        gpsModule.setGpsModuleCallback(mockCallback);

        // Inject NMEA test data
        byte[] buffer = NmeaSampleData.SAMPLE_BLOCKED_NO_FIX;
        gpsDevice.write(buffer, buffer.length);

        // Verify callback results
        Mockito.verify(mockCallback, times(5)).onNmeaMessage(anyString());
        Mockito.verify(mockCallback, never()).onGpsTimeUpdate(anyLong());
        Mockito.verify(mockCallback, never()).onGpsLocationUpdate(any(Location.class));
        Mockito.verify(mockCallback, never()).onGpsSatelliteStatus(any(GnssStatus.class));
    }

    @Test
    public void testGps_Satellites_NoFix() throws IOException {
        // Set up fake device
        LoopbackUartDevice gpsDevice = new LoopbackUartDevice();
        NmeaGpsModule gpsModule = new NmeaGpsModule(gpsDevice, DEFAULT_BAUD, DEFAULT_ACCURACY, null);

        GpsModuleCallback mockCallback = Mockito.mock(GpsModuleCallback.class);
        gpsModule.setGpsModuleCallback(mockCallback);

        // Inject NMEA test data
        byte[] buffer = NmeaSampleData.SAMPLE_SAT_VIEW_NO_FIX;
        gpsDevice.write(buffer, buffer.length);

        // Verify callback results
        Mockito.verify(mockCallback, times(6)).onNmeaMessage(anyString());
        Mockito.verify(mockCallback, never()).onGpsTimeUpdate(anyLong());
        Mockito.verify(mockCallback, never()).onGpsLocationUpdate(any(Location.class));

        ArgumentCaptor<GnssStatus> args = ArgumentCaptor.forClass(GnssStatus.class);
        Mockito.verify(mockCallback, times(1)).onGpsSatelliteStatus(args.capture());
        GnssStatus status = args.getValue();
        assertEquals(NmeaSampleData.EXPECTED_SAT_COUNT, status.getSatelliteCount());
    }

    @Test
    public void testGps_ValidFix() throws IOException {
        // Set up fake device
        LoopbackUartDevice gpsDevice = new LoopbackUartDevice();
        NmeaGpsModule gpsModule = new NmeaGpsModule(gpsDevice, DEFAULT_BAUD, DEFAULT_ACCURACY, null);

        GpsModuleCallback mockCallback = Mockito.mock(GpsModuleCallback.class);
        gpsModule.setGpsModuleCallback(mockCallback);

        // Inject NMEA test data
        byte[] buffer = NmeaSampleData.SAMPLE_VALID_FIX;
        gpsDevice.write(buffer, buffer.length);

        // Verify callback results
        Mockito.verify(mockCallback, times(6)).onNmeaMessage(anyString());
        Mockito.verify(mockCallback, times(1)).onGpsTimeUpdate(NmeaSampleData.EXPECTED_TIMESTAMP);

        ArgumentCaptor<GnssStatus> statusArgs = ArgumentCaptor.forClass(GnssStatus.class);
        Mockito.verify(mockCallback, times(1)).onGpsSatelliteStatus(statusArgs.capture());
        GnssStatus status = statusArgs.getValue();
        assertEquals(NmeaSampleData.EXPECTED_SAT_COUNT, status.getSatelliteCount());

        ArgumentCaptor<Location> locationArgs = ArgumentCaptor.forClass(Location.class);
        Mockito.verify(mockCallback, times(3)).onGpsLocationUpdate(locationArgs.capture());

        // Verify lat/lng for each report
        for (Location item : locationArgs.getAllValues()) {
            assertEquals(NmeaSampleData.EXPECTED_LATITUDE, item.getLatitude(), 0.0001);
            assertEquals(NmeaSampleData.EXPECTED_LONGITUDE, item.getLongitude(), 0.0001);
        }
    }

}
