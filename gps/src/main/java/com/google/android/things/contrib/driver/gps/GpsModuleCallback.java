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

import android.location.GnssStatus;
import android.location.Location;

/**
 * Callback invoked when new data is emitted from a
 * GPS module.
 */
public abstract class GpsModuleCallback {
    /**
     * Callback reporting GPS satellite status.
     *
     * @param status Latest status information from GPS module.
     */
    public abstract void onGpsSatelliteStatus(GnssStatus status);

    /**
     * Callback reporting an updated date/time from
     * the GPS satellite.
     *
     * @param timestamp Last received timestamp from GPS.
     */
    public abstract void onGpsTimeUpdate(long timestamp);

    /**
     * Callback reporting a location fix from the GPS module.
     *
     * @param location Latest location update.
     */
    public abstract void onGpsLocationUpdate(Location location);

    /**
     * Callback reporting raw NMEA sentences from the GPS module.
     *
     * @param nmeaMessage NMEA message data.
     */
    public abstract void onNmeaMessage(String nmeaMessage);
}
