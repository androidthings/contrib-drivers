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

package com.google.android.things.contrib.driver.gps;

import android.content.Context;
import android.location.GnssStatus;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.userdriver.location.GnssDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;

/**
 * User-space driver to process NMEA output from a UART-connected
 * GPS module and forward events to the Android location framework.
 */
@SuppressWarnings("WeakerAccess")
public class NmeaGpsDriver extends GnssDriver implements AutoCloseable {

    private Context mContext;
    private NmeaGpsModule mGpsModule;

    /**
     * Create a new NmeaGpsDriver to forward GPS location events to the
     * Android location framework.
     *
     * @param context Current context, used for loading resources
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     * @param accuracy Specified accuracy of the GPS module, in meters CEP.
     */
    public NmeaGpsDriver(Context context, String uartName, int baudRate,
                         float accuracy) throws IOException {
        this(context, uartName, baudRate, accuracy, null);
    }

    /**
     * Create a new NmeaGpsDriver to forward GPS location events to the
     * Android location framework.
     *
     * @param context Current context, used for loading resources
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     * @param accuracy Specified accuracy of the GPS module, in meters CEP.
     * @param handler optional {@link Handler} for software polling and callback events.
     */
    public NmeaGpsDriver(Context context, String uartName, int baudRate,
                         float accuracy, Handler handler) throws IOException {
        NmeaGpsModule module = new NmeaGpsModule(uartName, baudRate, accuracy, handler);
        init(context, module);
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ NmeaGpsDriver(Context context, NmeaGpsModule module) throws IOException {
        init(context, module);
    }

    /**
     * Initialize peripherals from the constructor.
     */
    private void init(Context context, NmeaGpsModule module) {
        mContext = context.getApplicationContext();
        mGpsModule = module;
        mGpsModule.setGpsModuleCallback(mCallback);
    }

    /**
     * Callback invoked by the GPS module when new data
     * arrives over the UART.
     */
    private GpsModuleCallback mCallback = new GpsModuleCallback() {
        @Override
        public void onGpsSatelliteStatus(GnssStatus status) {
            reportStatus(status);
        }

        @Override
        public void onGpsTimeUpdate(long timestamp) { }

        @Override
        public void onGpsLocationUpdate(Location location) {
            reportLocation(location);
        }

        @Override
        public void onNmeaMessage(String nmeaMessage) {
            reportNmea(nmeaMessage);
        }
    };

    /**
     * Register this driver with the Android location framework.
     */
    public void register() {
        UserDriverManager manager = UserDriverManager.getInstance();
        manager.registerGnssDriver(this);
    }

    /**
     * Unregister this driver with the Android location framework.
     */
    public void unregister() {
        UserDriverManager manager = UserDriverManager.getInstance();
        manager.unregisterGnssDriver();
    }

    /**
     * Close this driver and any underlying resources associated with the connection.
     */
    @Override
    public void close() throws IOException {
        unregister();
        if (mGpsModule != null) {
            mGpsModule.setGpsModuleCallback(null);
            try {
                mGpsModule.close();
            } finally {
                mGpsModule = null;
            }
        }
    }
}
