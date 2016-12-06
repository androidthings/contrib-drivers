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
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.userdriver.GpsDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;

/**
 * User-space driver to process NMEA output from a UART-connected
 * GPS module and forward events to the Android location framework.
 */
@SuppressWarnings("WeakerAccess")
public class NmeaGpsDriver implements AutoCloseable {

    private Context mContext;
    private GpsDriver mDriver;
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
        NmeaGpsModule module = new NmeaGpsModule(uartName, baudRate, handler);
        init(context, module, accuracy);
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ NmeaGpsDriver(Context context, NmeaGpsModule module) throws IOException {
        init(context, module, module.getGpsAccuracy());
    }

    /**
     * Initialize peripherals from the constructor.
     */
    private void init(Context context, NmeaGpsModule module, float accuracy) {
        mContext = context.getApplicationContext();
        mGpsModule = module;
        mGpsModule.setGpsAccuracy(accuracy);
        mGpsModule.setGpsModuleCallback(mCallback);
    }

    /**
     * Callback invoked by the GPS module when new data
     * arrives over the UART.
     */
    private Location mLastKnownLocation = new Location(LocationManager.GPS_PROVIDER);
    private GpsModuleCallback mCallback = new GpsModuleCallback() {
        @Override
        public void onGpsSatelliteStatus(boolean active, int satellites) { }

        @Override
        public void onGpsTimeUpdate(long timestamp) { }

        @Override
        public void onGpsPositionUpdate(long timestamp,
                                        double latitude, double longitude, double altitude) {
            if (mDriver != null) {
                mLastKnownLocation.setTime(timestamp);
                // We cannot compute accuracy from NMEA data alone.
                // Assume that a valid fix has the quoted accuracy of the module.
                // Framework requires accuracy in DRMS.
                mLastKnownLocation.setAccuracy(mGpsModule.getGpsAccuracy() * 1.2f);


                mLastKnownLocation.setLatitude(latitude);
                mLastKnownLocation.setLongitude(longitude);
                if (altitude != -1) {
                    mLastKnownLocation.setAltitude(altitude);
                } else {
                    mLastKnownLocation.removeAltitude();
                }
                mLastKnownLocation.removeSpeed();
                mLastKnownLocation.removeBearing();

                // Is the location update ready to send?
                if (mLastKnownLocation.hasAccuracy() && mLastKnownLocation.getTime() != 0) {
                    mDriver.reportLocation(mLastKnownLocation);
                }
            }
        }


        @Override
        public void onGpsSpeedUpdate(float speed, float bearing) {
            if (mDriver != null) {
                mLastKnownLocation.setSpeed(speed);
                mLastKnownLocation.setBearing(bearing);

                // Is the location update ready to send?
                if (mLastKnownLocation.hasAccuracy() && mLastKnownLocation.getTime() != 0) {
                    mDriver.reportLocation(mLastKnownLocation);
                }
            }
        }
    };

    /**
     * Register this driver with the Android location framework.
     */
    public void register() {
        if (mDriver == null) {
            UserDriverManager manager = UserDriverManager.getManager();
            mDriver = new GpsDriver();
            manager.registerGpsDriver(mDriver);
        }
    }

    /**
     * Unregister this driver with the Android location framework.
     */
    public void unregister() {
        if (mDriver != null) {
            UserDriverManager manager = UserDriverManager.getManager();
            manager.unregisterGpsDriver();
            mDriver = null;
        }
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
