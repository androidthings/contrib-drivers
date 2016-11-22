package com.google.brillo.driver.speaker;

import android.hardware.pio.PeripheralManagerService;
import android.hardware.pio.Pwm;

import java.io.IOException;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Speaker implements AutoCloseable {

    private Pwm mPwm;

    /**
     * Create a Speaker from a {@link Pwm} device
     */
    public Speaker(Pwm device) throws IOException {
        connect(device);
    }

    /**
     * Create a Speaker connected to the given Pwm pin name
     */
    public Speaker(String pin) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Pwm device = pioService.openPwm(pin);
        try {
            connect(device);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    private void connect(Pwm device) throws IOException {
        mPwm = device;
        mPwm.setPwmDutyCycle(50.0); // square wave
    }

    @Override
    public void close() throws IOException {
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
     * @throws IOException
     * @throws IllegalStateException if the device is closed
     */
    public void play(double frequency) throws IOException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.setPwmFrequencyHz(frequency);
        mPwm.enable();
    }

    /**
     * Stop a currently playing frequency
     *
     * @throws IOException
     * @throws IllegalStateException if the device is closed
     */
    public void stop() throws IOException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.disable();
    }
}
