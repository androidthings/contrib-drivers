package com.google.brillo.driver.servo;

import android.os.RemoteException;
import android.pio.PeripheralManagerService;
import android.pio.Pwm;
import android.system.ErrnoException;
import android.util.Log;

import java.io.Closeable;

@SuppressWarnings("WeakerAccess")
public class Servo implements Closeable {
    private static final String TAG = Servo.class.getSimpleName();

    private static final double DEFAULT_MIN_PULSE_DURATION_MS = 1.0;
    private static final double DEFAULT_MAX_PULSE_DURATION_MS = 2.0;
    private static final double DEFAULT_MIN_ANGLE_DEG = 0.0;
    private static final double DEFAULT_MAX_ANGLE_DEG = 180.0;
    private static final double DEFAULT_FREQUENCY_HZ = 50;

    private final PeripheralManagerService mPioService;

    private Pwm mPwm;
    private boolean mEnabled = false;
    private double mPulseDurationMin; // milliseconds
    private double mPulseDurationMax; // milliseconds
    private double mAngleMin; // degrees
    private double mAngleMax; // degrees
    private double mPeriod; // milliseconds

    public Servo(PeripheralManagerService pioService) {
        mPioService = pioService;
        mPulseDurationMin = DEFAULT_MIN_PULSE_DURATION_MS;
        mPulseDurationMax = DEFAULT_MAX_PULSE_DURATION_MS;
        mAngleMin = DEFAULT_MIN_ANGLE_DEG;
        mAngleMax = DEFAULT_MAX_ANGLE_DEG;
    }

    public void open(String pin) throws RemoteException, ErrnoException {
        open(pin, DEFAULT_FREQUENCY_HZ);
    }

    public void open(String pin, double frequencyHz) throws RemoteException, ErrnoException {
        mPwm = mPioService.openPwm(pin);
        mPwm.setPwmFrequencyHz(frequencyHz);
        mPeriod = 1000.0 / frequencyHz;
    }

    public void close() throws IllegalStateException {
        if (mPwm != null) {
            mPwm.close();
            mPwm = null;
        }
    }

    /**
     * Set servo rotation.
     *
     * @param angleDeg Angle in degree, between {@link #DEFAULT_MIN_ANGLE_DEG}
     *                 and {@link #DEFAULT_MAX_ANGLE_DEG}.
     */
    public void set(double angleDeg) throws RemoteException, ErrnoException  {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        if (angleDeg < mAngleMin || angleDeg > mAngleMax) {
            throw new IllegalArgumentException("angleDeg (" + angleDeg + ") outside the range [" +
                    mAngleMin + ", " + mAngleMax + "]");
        }
        if (!mEnabled) {
            enable();
        }
        // normalize angle ratio.
        double t = (angleDeg - mAngleMin) / (mAngleMax - mAngleMin);
        t %= 1.0;
        // linearly interpolate angle between servo ranges to get a pulse width in milliseconds.
        double pw = mPulseDurationMin + (mPulseDurationMax - mPulseDurationMin) * t;

        // convert the pulse width into a percentage of the mPeriod of the wave form.
        double dutyCycle = 100 * pw / mPeriod;

        Log.v(TAG, String.format("angleDeg=%f  t=%f  pw=%f  dutyCycle=%f", angleDeg, t, pw, dutyCycle));

        mPwm.setPwmDutyCycle(dutyCycle);
    }

    public void enable() throws RemoteException, ErrnoException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.enable();
        mEnabled = true;
    }

    public void disable() throws RemoteException, ErrnoException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.disable();
        mEnabled = false;
    }

    public void setPulseDurationRange(double minMs, double maxMs) {
        mPulseDurationMin = minMs;
        mPulseDurationMax = maxMs;
    }

    public void setAngleRange(double minDeg, double maxDeg) {
        mAngleMin = minDeg;
        mAngleMax = maxDeg;
    }
}