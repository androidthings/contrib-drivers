package com.google.android.things.contrib.driver.output;

import android.view.InputDevice;
import android.view.KeyEvent;

import com.google.android.things.userdriver.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;


public class OutputDriver extends  Output implements AutoCloseable {
    private static final String TAG = OutputDriver.class.getSimpleName();
    private static final String DRIVER_NAME = "Output";
    private static final int DRIVER_VERSION = 1;

    private InputDriver mDriver;
    private int mKeycode;

    /**
     * Create a new framework input driver for a output connected on given GPIO pin.
     * The driver emits {@link android.view.KeyEvent} with the given keycode when registered.
     * @param pin GPIO pin where the button is connected.
     * @param keycode keycode to be emitted.
     * @throws IOException
     * @see #register
     * @return new input driver instance.
     */
    public OutputDriver(String pin, int keycode) throws IOException {
        super(pin);
        mKeycode = keycode;
    }

    /**
     * Register the driver in the framework.
     */
    public void register() {
        if (mDriver == null) {
            mDriver = build(mKeycode);
            UserDriverManager.getManager().registerInputDriver(mDriver);
        }
    }

    /**
     * Unregister the driver from the framework.
     */
    public void unregister() {
        if (mDriver != null) {
            UserDriverManager.getManager().unregisterInputDriver(mDriver);
            mDriver = null;
        }
    }

    @Override
    public void close() throws IOException {
        unregister();
        super.close();
    }

    private InputDriver build(final int keyCode) {
        final InputDriver inputDriver = new InputDriver.Builder(InputDevice.SOURCE_CLASS_BUTTON)
                .setName(DRIVER_NAME)
                .setVersion(DRIVER_VERSION)
                .setKeys(new int[]{keyCode})
                .build();

        super.setOnOutputEventListener(new OnOutputEventListener() {
             @Override
             public void onOutputEvent(boolean pressed) {
                 int keyAction = pressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
                 inputDriver.emit(new KeyEvent[]{
                         new KeyEvent(keyAction, keyCode)
                 });
             }
        });

        return inputDriver;
    }
}
