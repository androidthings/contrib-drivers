package com.google.brillo.driver.servo;

import android.hardware.pio.PeripheralManagerService;
import android.hardware.pio.Pwm;
import android.system.ErrnoException;
import android.util.Log;

import java.io.Closeable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Servo implements Closeable {
    private static final String TAG = Servo.class.getSimpleName();

    public static final double DEFAULT_FREQUENCY_HZ = 50;

    private static final double DEFAULT_MIN_PULSE_DURATION_MS = 1.0;
    private static final double DEFAULT_MAX_PULSE_DURATION_MS = 2.0;
    private static final double DEFAULT_MIN_ANGLE_DEG = 0.0;
    private static final double DEFAULT_MAX_ANGLE_DEG = 180.0;

    private Pwm mPwm;
    private boolean mEnabled = false;
    private double mPulseDurationMin = DEFAULT_MIN_PULSE_DURATION_MS; // milliseconds
    private double mPulseDurationMax = DEFAULT_MAX_PULSE_DURATION_MS; // milliseconds
    private double mAngleMin = DEFAULT_MIN_ANGLE_DEG; // degrees
    private double mAngleMax = DEFAULT_MAX_ANGLE_DEG; // degrees
    private double mPeriod; // milliseconds

    public Servo(String pin) throws ErrnoException {
        this(pin, DEFAULT_FREQUENCY_HZ);
    }

    public Servo(String pin, double frequencyHz) throws ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Pwm device = pioService.openPwm(pin);
        try {
            connect(device, frequencyHz);
        } catch (ErrnoException|RuntimeException e) {
            close();
            throw e;
        }
    }

    public Servo(Pwm device, double frequencyHz) throws ErrnoException {
        connect(device, frequencyHz);
    }

    private void connect(Pwm device, double frequencyHz) throws ErrnoException {
        mPwm = device;
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
    public void set(double angleDeg) throws ErrnoException  {
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

    public void enable() throws ErrnoException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.enable();
        mEnabled = true;
    }

    public void disable() throws ErrnoException {
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