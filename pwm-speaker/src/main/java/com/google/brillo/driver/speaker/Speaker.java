package com.google.brillo.driver.speaker;

import android.hardware.pio.PeripheralManagerService;
import android.hardware.pio.Pwm;
import android.system.ErrnoException;

import java.io.Closeable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Speaker implements Closeable {

    private Pwm mPwm;

    /**
     * Create a Speaker from a {@link Pwm} device
     */
    public Speaker(Pwm device) throws ErrnoException {
        connect(device);
    }

    /**
     * Create a Speaker connected to the given Pwm pin name
     */
    public Speaker(String pin) throws ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Pwm device = pioService.openPwm(pin);
        try {
            connect(device);
        } catch (ErrnoException|RuntimeException e) {
            close();
            throw e;
        }
    }

    private void connect(Pwm device) throws ErrnoException {
        mPwm = device;
        mPwm.setPwmDutyCycle(50.0); // square wave
    }

    @Override
    public void close() {
        if (mPwm != null) {
            try {
                mPwm.close();
            } finally {
                mPwm = null;
            }
        }
    }

    /**
     * Play the specified frequency. Play continues until {@link #stop()} is called.
     *
     * @param frequency the frequency to play in Hz
     * @throws ErrnoException
     * @throws IllegalStateException if the device is closed
     */
    public void play(double frequency) throws ErrnoException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.setPwmFrequencyHz(frequency);
        mPwm.enable();
    }

    /**
     * Stop a currently playing frequency
     *
     * @throws ErrnoException
     * @throws IllegalStateException if the device is closed
     */
    public void stop() throws ErrnoException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.disable();
    }
}
