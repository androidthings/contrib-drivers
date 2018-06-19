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
import android.location.LocationManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.things.userdriver.location.GnssStatusBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class to handle converting NMEA strings into
 * location components.
 */
/*package*/ class NmeaParser {

    // Message framing characters
    private static final byte FRAME_START = 0x24;    // $
    private static final byte CHECKSUM_START = 0x2A; // *
    private static final byte FRAME_END = 0x0D;      // CR
    private static final String DELIMITER = ",";

    // NMEA Message Types
    private static final String GGA = "GPGGA";
    private static final String GSV = "GPGSV";
    private static final String GLL = "GPGLL";
    private static final String RMC = "GPRMC";

    private Calendar mTimestampCalendar;
    private GpsModuleCallback mGpsModuleCallback;

    private float mGpsAccuracy;

    /*package*/ NmeaParser(float gpsAccuracy) {
        // Initialize timestamp calendar to current time
        mTimestampCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        mGpsAccuracy = gpsAccuracy;
    }

    /*package*/ void setGpsModuleCallback(GpsModuleCallback callback) {
        mGpsModuleCallback = callback;
    }

    /*package*/ byte getFrameStart() {
        return FRAME_START;
    }

    /*package*/ byte getFrameEnd() {
        return FRAME_END;
    }

    /*package*/ void processMessageFrame(byte[] message) throws ParseException {
        if (message == null || message.length < 1) {
            throw new ParseException("Invalid message frame", 0);
        }

        // Validate the checksum
        int index = validateChecksum(message);

        // Report the raw, validated message
        String nmea = new String(message, 0, index);
        postRaw(nmea);

        // Parse the message based on type
        String[] tokens = nmea.split(DELIMITER);
        switch (tokens[0]) {
            case GGA:
                handleFixInformation(tokens);
                break;
            case GSV:
                handleSatelliteData(tokens);
                break;
            case GLL:
                handleLatLngData(tokens);
                break;
            case RMC:
                handleRecommendedMinimum(tokens);
                break;
            default:
                // Ignore the message
        }
    }

    /**
     * Validate the message contents against the checksum.
     */
    private int validateChecksum(byte[] message) throws ParseException {
        int index = 0;
        int messageSum = message[index++];
        while (index < message.length) {
            if (message[index] == CHECKSUM_START) {
                break;
            }

            messageSum ^= message[index++];
        }

        // Index is pointing to checksum start
        if (index >= (message.length - 2)) {
            throw new ParseException("Checksum missing from incoming message", index);
        }

        int checkSum = convertAsciiByte(message[index+1], message[index+2]);
        if (messageSum != checkSum) {
            throw new ParseException("Invalid checksum (" + messageSum + "), expected " + checkSum, index);
        }

        return index;
    }

    /**
     * Parse the contents of a GPGGA sentence
     * @param nmea Sentence tokens
     */
    private void handleFixInformation(String[] nmea) throws ParseException {
        if (nmea.length < 12) {
            throw new ParseException("Invalid GGA Message", nmea.length);
        }

        int quality = Integer.parseInt(nmea[6]);
        if (quality < 1) {
            // No valid fix
            return;
        }

        long timestamp = getUpdatedTimestamp(nmea[1], null);
        double latitude = parseCoordinate(nmea[2], nmea[3]);
        double longitude = parseCoordinate(nmea[4], nmea[5]);
        double altitude = parseDistance(nmea[9], nmea[10]);
        double seaLevel = parseDistance(nmea[11], nmea[12]);
        postLocation(timestamp, latitude, longitude, altitude - seaLevel, -1, -1);
    }

    /** Intermediate representation of satellite data */
    private SparseArray<Satellite> mSatellites = new SparseArray<>();
    private static class Satellite {
        int svid;
        float elevation;
        float azimuth;
        float snr;
    }
    /**
     * Parse the contents of a GPGSV sentence
     * @param nmea Sentence tokens
     */
    private void handleSatelliteData(String[] nmea) throws ParseException {
        if (nmea.length < 4) {
            throw new ParseException("Invalid GSV Message", nmea.length);
        }

        int satelliteCount = Integer.parseInt(nmea[3]);
        if (satelliteCount < 1) {
            // No valid fix
            return;
        }

        // Parse the satellites in this message
        for (int i = 4; (i + 3) < nmea.length; i += 4) {
            if (!nmea[i].isEmpty()) {
                Satellite sat = new Satellite();
                sat.svid = Integer.parseInt(nmea[i]);
                sat.elevation = parseTrackAngle(nmea[i+1]);
                sat.azimuth = parseTrackAngle(nmea[i+2]);
                sat.snr = parseSignal(nmea[i+3]);

                mSatellites.put(sat.svid, sat);
            }
        }

        if (mSatellites.size() < satelliteCount) {
            // We haven't yet received all the satellite data
            return;
        }

        postSatelliteStatus(mSatellites);
        mSatellites.clear();
    }

    /**
     * Parse the contents of a GPGLL sentence
     * @param nmea Sentence tokens
     */
    private void handleLatLngData(String[] nmea) throws ParseException {
        if (nmea.length < 7) {
            throw new ParseException("Invalid GLL Message", nmea.length);
        }

        String status = nmea[6];
        if (status.contains("V")) {
            // No valid fix
            return;
        }

        long timestamp = getUpdatedTimestamp(nmea[5], null);
        double latitude = parseCoordinate(nmea[1], nmea[2]);
        double longitude = parseCoordinate(nmea[3], nmea[4]);
        postLocation(timestamp, latitude, longitude, -1, -1, -1);
    }

    /**
     * Parse the contents of a GPRMC sentence
     * @param nmea Sentence tokens
     */
    private void handleRecommendedMinimum(String[] nmea) throws ParseException {
        if (nmea.length < 11) {
            throw new ParseException("Invalid RMC Message", nmea.length);
        }

        String status = nmea[2];
        if (status.contains("V")) {
            // No valid fix
            return;
        }

        long timestamp = getUpdatedTimestamp(nmea[1], nmea[9]);
        postTime(timestamp);

        double latitude = parseCoordinate(nmea[3], nmea[4]);
        double longitude = parseCoordinate(nmea[5], nmea[6]);
        float speed = parseSpeed(nmea[7], "N");
        float bearing = parseTrackAngle(nmea[8]);
        postLocation(timestamp, latitude, longitude, -1, speed, bearing);
    }

    private void postRaw(String rawNmea) {
        if (mGpsModuleCallback != null) {
            mGpsModuleCallback.onNmeaMessage(rawNmea);
        }
    }

    private void postTime(long timestamp) {
        if (mGpsModuleCallback != null) {
            mGpsModuleCallback.onGpsTimeUpdate(timestamp);
        }
    }

    private void postSatelliteStatus(SparseArray<Satellite> satellites) {
        if (mGpsModuleCallback != null) {
            GnssStatusBuilder builder = new GnssStatusBuilder(satellites.size())
                    .setFlags(0, GnssStatusBuilder.GNSS_SV_FLAGS_NONE)
                    .setConstellation(0, GnssStatus.CONSTELLATION_GPS);

            for (int i = 0; i < satellites.size(); i++) {
                Satellite sat = satellites.valueAt(i);
                builder.setSvid(i, sat.svid)
                        .setElevation(i, sat.elevation)
                        .setAzimuth(i, sat.azimuth)
                        .setCn0DbHz(i, sat.snr);
            }

            mGpsModuleCallback.onGpsSatelliteStatus(builder.build());
        }
    }

    private void postLocation(long timestamp, double latitude, double longitude, double altitude, float speed, float bearing) {
        if (mGpsModuleCallback != null) {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            // We cannot compute accuracy from NMEA data alone.
            // Assume that a valid fix has the quoted accuracy of the module.
            // Framework requires accuracy in DRMS.
            location.setAccuracy(mGpsAccuracy * 1.2f);
            location.setTime(timestamp);

            location.setLatitude(latitude);
            location.setLongitude(longitude);
            if (altitude != -1) {
                location.setAltitude(altitude);
            }
            if (speed != -1) {
                location.setSpeed(speed);
            }
            if (bearing != -1) {
                location.setBearing(bearing);
            }

            mGpsModuleCallback.onGpsLocationUpdate(location);
        }
    }

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("ddMMyyHHmmss", Locale.US);
    /**
     * Apply the NMEA time parameters and get an updated timestamp
     * @param timeString NMEA fix time string
     * @param dateString NMEA date string, optional
     * @return Epoch timestamp.
     */
    private long getUpdatedTimestamp(String timeString, String dateString) {
        if (timeString.length() < 6) {
            // Invalid time
            return -1;
        }
        if (dateString != null && dateString.length() < 6) {
            // Invalid date
            return -1;
        }


        try {
            // Use last known date if not supplied
            if (dateString == null) {
                dateString = DateFormat.format("ddMMyy", mTimestampCalendar).toString();
            }
            // Truncate milliseconds
            int pointIndex = timeString.indexOf('.');
            if (pointIndex != -1) {
                timeString = timeString.substring(0, pointIndex);
            }

            Date date = FORMAT.parse(dateString+timeString);
            mTimestampCalendar.setTime(date);
            return mTimestampCalendar.getTimeInMillis();
        } catch (ParseException e) {
            // Default to current time
            return System.currentTimeMillis();
        }
    }

    /**
     * Combine an NMEA coordinate tuple into a decimal value in degrees.
     * @param degreeString Coordinate in degrees, minutes, seconds.
     * @param hemisphere Hemisphere designation (N,S,E,W)
     * @return Decimal value of the coordinate.
     */
    private double parseCoordinate(String degreeString, String hemisphere) {
        if (degreeString.isEmpty() || hemisphere.isEmpty()) {
            // No data
            return -1;
        }

        // Two digits left of decimal to the end are the minutes
        int index = degreeString.indexOf('.') - 2;
        if (index < 0) {
            // Invalid string
            return -1;
        }

        // Parse full degrees
        try {
            double value = Double.parseDouble(degreeString.substring(0, index));
            // Append the minutes
            value += Double.parseDouble(degreeString.substring(index)) / 60.0;

            // Compensate for the hemisphere
            if (hemisphere.contains("W") || hemisphere.contains("S")) {
                value *= -1;
            }

            return value;
        } catch (NumberFormatException e) {
            // Invalid value
            return -1;
        }
    }

    /**
     * Convert a Signal-Noise Ratio string into a decimal value in dB.
     * @param signalString Integer string of the SNR.
     * @return Decimal value, or -1 if null
     */
    private float parseSignal(String signalString) {
        if (signalString.isEmpty()) {
            return -1;
        }

        try {
            return Float.parseFloat(signalString);
        } catch (NumberFormatException e) {
            // Invalid value
            return -1;
        }
    }

    /**
     * Convert an NMEA angle string into a decimal value in true degrees.
     * @param angleString Decimal string of the angle.
     * @return Decimal value converted to true degrees.
     */
    private float parseTrackAngle(String angleString) {
        if (angleString.isEmpty()) {
            return -1;
        }

        try {
            return Float.parseFloat(angleString);
        } catch (NumberFormatException e) {
            // Invalid value
            return -1;
        }
    }

    /**
     * Combine an NMEA distance tuple into a decimal value in meters.
     * @param distString Decimal string of the distance.
     * @param unit Unit of measure constant.
     * @return Decimal value converted to meters.
     */
    private double parseDistance(String distString, String unit) {
        if (distString.isEmpty() || unit.isEmpty()) {
            // No data
            return -1;
        }

        try {
            double value = Double.parseDouble(distString);

            switch (unit) {
                case "M": // meters
                    return value;
                case "K": // kilometers
                    return value / 1000f;
                default:
                    // Unsupported unit of measure
                    return -1;
            }
        } catch (NumberFormatException e) {
            // Invalid value
            return -1;
        }
    }

    private static final float KNOTS = 0.514444f;
    private static final float KPH = 0.277778f;
    /**
     * Combine an NMEA speed tuple into a decimal value in m/s.
     * @param speedString Decimal string of the speed.
     * @param unit Unit of measure constant.
     * @return Decimal value converted to m/s.
     */
    private float parseSpeed(String speedString, String unit) {
        if (speedString.isEmpty() || unit.isEmpty()) {
            // No data
            return -1;
        }

        try {
            float value = Float.parseFloat(speedString);

            switch (unit) {
                case "N": // knots
                    return value * KNOTS;
                case "K": // kilometers/hour
                    return value * KPH;
                default:
                    // Unsupported unit of measure
                    return -1;
            }
        } catch (NumberFormatException e) {
            // Invalid value
            return -1;
        }
    }

    /**
     * Convert a 2-byte ASCII hex value into the equivalent numeric value.
     * @param msb High byte representing the most significant nibble.
     * @param lsb Low byte representing the least significant nibble.
     * @return combined numeric value.
     */
    private int convertAsciiByte(byte msb, byte lsb) {
        return (getHexDigit(msb) << 4) | getHexDigit(lsb);
    }

    /**
     * Convert an ASCII hex digit into its numeric value.
     * @param b ASCII code representing the hex digit (0-9,A-F)
     * @return numeric value of the digit.
     */
    private byte getHexDigit(byte b) {
        if (b >= 0x30 && b <= 0x39) { // 0-9
            return (byte) (b - 0x30);
        }
        if (b >= 0x41 && b <= 0x46) { // A-F
            return (byte) (b - 0x37);
        }

        throw new IllegalArgumentException("Invalid ASCII hex byte");
    }
}
