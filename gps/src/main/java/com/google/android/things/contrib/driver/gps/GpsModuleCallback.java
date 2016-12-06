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

/**
 * Callback invoked when new data is emitted from a
 * GPS module.
 */
public abstract class GpsModuleCallback {
    /**
     * Callback reporting GPS satellite status.
     *
     * @param active true if module has locked enough satellites to get a fix,
     *               false otherwise.
     * @param satellites Number of satellites the module has locked.
     */
    public abstract void onGpsSatelliteStatus(boolean active, int satellites);

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
     * @param timestamp Timestamp of the fix, in milliseconds.
     * @param latitude Latitude, in degrees
     * @param longitude Longitude, in degrees
     * @param altitude Altitude, in meters, above WGS-84.
     *                 Will be -1 if altitude is not available.
     */
    public abstract void onGpsPositionUpdate(long timestamp, double latitude, double longitude, double altitude);

    /**
     * Callback reporting speed and heading information from
     * the GPS module.
     * @param speed Speed, in meters per second.
     * @param bearing Heading, in degrees.
     */
    public abstract void onGpsSpeedUpdate(float speed, float bearing);
}
