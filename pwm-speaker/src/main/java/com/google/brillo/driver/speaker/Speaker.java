package com.google.brillo.driver.speaker;

import android.hardware.pio.PeripheralManagerService;
import android.hardware.pio.Pwm;
import android.system.ErrnoException;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Speaker implements Closeable {
    private boolean mPlaying = false;
    private Pwm mPwm;
    private final Map<String, Double> mNotes = new HashMap<>();

    public Speaker() {
        addNote("C", 261.6255653);
        addNote("C#", 277.182631);
        addNote("D", 293.6647679);
        addNote("D#",  311.1269837);
        addNote("E",  329.6275569);
        addNote("F",  349.2282314);
        addNote("F#",  369.9944227);
        addNote("G",  391.995436);
        addNote("G#",  415.3046976);
        addNote("A",  440);
        addNote("A#",  466.1637615);
        addNote("B", 493.8833013);
    }

    public void open(String pin) throws ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        mPwm = pioService.openPwm(pin);
        mPwm.setPwmDutyCycle(50.0); // square wave.
    }

    public void close() throws IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.close();
    }

    public void play(String note, long durationMs)
            throws ErrnoException, IllegalStateException, InterruptedException {
        play(note);
        Thread.sleep(durationMs);
        stop();
    }

    public void play(String note)
            throws ErrnoException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        if (!mPlaying) {
            mPwm.enable();
            mPlaying = true;
        }
        mPwm.setPwmFrequencyHz(mNotes.get(note));
    }

    public void addNote(String note, double freq) {
        mNotes.put(note, freq);
    }

    public void stop() throws ErrnoException, IllegalStateException {
        if (mPwm == null) {
            throw new IllegalStateException("pwm device not opened");
        }
        mPwm.disable();
        mPlaying = false;
    }
}
