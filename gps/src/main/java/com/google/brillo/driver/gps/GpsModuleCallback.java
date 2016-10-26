package com.google.brillo.driver.gps;

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
