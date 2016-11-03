package com.google.brillo.driver.button;

import android.hardware.pio.Gpio;
import android.hardware.pio.GpioCallback;
import android.hardware.pio.PeripheralManagerService;
import android.hardware.userdriver.InputDriver;
import android.hardware.userdriver.InputDriverEvent;
import android.system.ErrnoException;
import android.util.Log;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * Driver for GPIO based buttons with pull-up or pull-down resistors.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Button implements Closeable {
    private static final String TAG = Button.class.getSimpleName();

    /**
     * Logic level when the button is considered pressed.
     */
    public enum LogicState {
        PRESSED_WHEN_HIGH,
        PRESSED_WHEN_LOW
    }
    private Gpio mButtonGpio;
    private GpioCallback mInterruptCallback;
    private OnButtonEventListener mListener;

    /**
     * Interface definition for a callback to be invoked when a Button event occurs.
     */
    public interface OnButtonEventListener {
        /**
         * Called when a Button event occurs
         *
         * @param button the Button for which the event occurred
         * @param pressed true if the Button is now pressed
         * @return true to continue receiving events from the Button. Returning false will stop
         * <b>all</b> future events from this Button.
         */
        boolean onButtonEvent(Button button, boolean pressed);
    }

    /**
     * Create a new Button driver for the givin GPIO pin name.
     * @param pin Gpio where the button is attached.
     * @param logicLevel Logic level when the button is considered pressed.
     * @throws ErrnoException
     */
    public Button(String pin, LogicState logicLevel) throws ErrnoException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Gpio buttonGpio = pioService.openGpio(pin);
        try {
            connect(buttonGpio, logicLevel);
        } catch (ErrnoException|RuntimeException e) {
            close();
            throw e;
        }
    }

    /**
     * Create a new Button driver for the given Gpio connection.
     * @param buttonGpio Gpio where the button is attached.
     * @param logicLevel Logic level when the button is considered pressed.
     * @throws ErrnoException
     */
    public Button(Gpio buttonGpio, LogicState logicLevel) throws ErrnoException {
       connect(buttonGpio, logicLevel);
    }

    private void connect(Gpio buttonGpio, LogicState logicLevel) throws ErrnoException {
        mButtonGpio = buttonGpio;
        mButtonGpio.setDirection(Gpio.DIRECTION_IN);
        mButtonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
        mInterruptCallback = new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                if (mListener == null) {
                    return true;
                }
                try {
                    boolean state = gpio.getValue();
                    if (logicLevel == LogicState.PRESSED_WHEN_HIGH) {
                        return mListener.onButtonEvent(Button.this, state);

                    }
                    if (logicLevel == LogicState.PRESSED_WHEN_LOW) {
                        return mListener.onButtonEvent(Button.this, !state);
                    }
                } catch (ErrnoException e) {
                    Log.e(TAG, "pio error: ", e);
                }
                return true;
            }
        };
        mButtonGpio.registerGpioCallback(mInterruptCallback);
    }

    /**
     * @return the underlying {@link Gpio} device
     */
    public Gpio getGpio() {
        return mButtonGpio;
    }

    /**
     * Set the listener to be called when a button event occured.
     * @param listener button event listener to be invoked.
     */
    public void setOnButtonEventListener(OnButtonEventListener listener) {
        mListener = listener;
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() {
        if (mButtonGpio != null) {
            mListener = null;
            mButtonGpio.unregisterGpioCallback(mInterruptCallback);
            mInterruptCallback = null;
            mButtonGpio.close();
            mButtonGpio = null;
        }
    }

    static class ButtonInputDriver {
        private static final String DRIVER_NAME = "Button";
        private static final int DRIVER_VERSION = 1;
        private static final int EV_SYN = 0;
        private static final int EV_KEY = 1;
        private static final int BUTTON_RELEASED = 0;
        private static final int BUTTON_PRESSED = 1;
        private static Integer[] SUPPORTED_EVENT_TYPE = {EV_SYN, EV_KEY};
        // temporary uvent constant until they get added to the framework.
        private static final int UI_SET_EVBIT = 1074025828;
        private static final int UI_SET_KEYBIT = 1074025829;
        static InputDriver build(Button button, int key) {
            Map<Integer, Integer[]> supportedKey = new HashMap<>();
            supportedKey.put(UI_SET_EVBIT, SUPPORTED_EVENT_TYPE);
            Integer[] keys = {key};
            supportedKey.put(UI_SET_KEYBIT, keys);
            InputDriver inputDriver = InputDriver.builder(supportedKey)
                    .name(DRIVER_NAME)
                    .version(DRIVER_VERSION)
                    .build();
            button.setOnButtonEventListener(new OnButtonEventListener() {
                @Override
                public boolean onButtonEvent(Button b, boolean pressed) {
                    int keyState = pressed ? BUTTON_PRESSED : BUTTON_RELEASED;
                    inputDriver.emit(new InputDriverEvent[]{
                            new InputDriverEvent(EV_KEY, key, keyState),
                            new InputDriverEvent(EV_SYN, 0, 0)
                    });
                    return true;
                }
            });
            return inputDriver;
        }
    }

    /**
     * Create a new {@link android.hardware.userdriver.InputDriver} that will emit
     * the proper key events whenever the {@link Button} is pressed or released.
     * Register this driver with the framework by calling {@link android.hardware.userdriver.UserDriverManager#registerInputDriver(InputDriver)}
     * @param key key to be emitted.
     * @return new input driver instance.
     * @see android.hardware.userdriver.UserDriverManager#registerInputDriver(InputDriver)
     */
    public InputDriver createInputDriver(int key) {
        return ButtonInputDriver.build(this, key);
    }
}