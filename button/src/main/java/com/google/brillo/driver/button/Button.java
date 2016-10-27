package com.google.brillo.driver.button;

import android.hardware.pio.Gpio;
import android.hardware.pio.GpioCallback;
import android.hardware.pio.PeripheralManagerService;
import android.hardware.userdriver.InputDriver;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.InputDevice;
import android.view.ViewConfiguration;
import android.view.KeyEvent;

import java.io.Closeable;
import java.io.IOException;

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
    private OnButtonEventListener mListener;

    private Handler mDebounceHandler;
    private CheckDebounce mPendingCheckDebounce;
    private long mDebounceDelay = ViewConfiguration.getTapTimeout();

    /**
     * Interface definition for a callback to be invoked when a Button event occurs.
     */
    public interface OnButtonEventListener {
        /**
         * Called when a Button event occurs
         *
         * @param button the Button for which the event occurred
         * @param pressed true if the Button is now pressed
         */
        void onButtonEvent(Button button, boolean pressed);
    }

    /**
     * Create a new Button driver for the given GPIO pin name.
     * @param pin Gpio where the button is attached.
     * @param logicLevel Logic level when the button is considered pressed.
     * @throws IOException
     */
    public Button(String pin, LogicState logicLevel) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Gpio buttonGpio = pioService.openGpio(pin);
        try {
            connect(buttonGpio, logicLevel);
        } catch (IOException|RuntimeException e) {
            close();
            throw e;
        }
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ Button(Gpio buttonGpio, LogicState logicLevel) throws IOException {
       connect(buttonGpio, logicLevel);
    }

    private void connect(Gpio buttonGpio, LogicState logicLevel) throws IOException {
        mButtonGpio = buttonGpio;
        mButtonGpio.setDirection(Gpio.DIRECTION_IN);
        mButtonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
        // Configure so pressed is always true
        mButtonGpio.setActiveType(logicLevel == LogicState.PRESSED_WHEN_LOW ?
                Gpio.ACTIVE_LOW : Gpio.ACTIVE_HIGH);
        mButtonGpio.registerGpioCallback(mInterruptCallback);

        mDebounceHandler = new Handler();
    }

    /**
     * Local callback to monitor GPIO edge events.
     */
    private GpioCallback mInterruptCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                boolean currentState = gpio.getValue();

                if (mDebounceDelay == 0) {
                    // Trigger event immediately
                    performButtonEvent(currentState);
                } else {
                    // Pass trigger state forward if a check was pending
                    boolean trigger = (mPendingCheckDebounce == null) ?
                            currentState : mPendingCheckDebounce.getTriggerState();
                    // Clear any pending checks
                    removeDebounceCallback();
                    // Set a new pending check
                    mPendingCheckDebounce = new CheckDebounce(trigger);
                    mDebounceHandler.postDelayed(mPendingCheckDebounce, mDebounceDelay);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading button state", e);
            }

            return true;
        }
    };

    /**
     * Set the listener to be called when a button event occurred.
     *
     * @param listener button event listener to be invoked.
     */
    public void setOnButtonEventListener(OnButtonEventListener listener) {
        mListener = listener;
    }

    /**
     * Set the time delay after an edge trigger that the button
     * must remain stable before generating an event. Debounce
     * is enabled by default for 100ms.
     *
     * Setting this value to zero disables debounce and triggers
     * events on all edges immediately.
     *
     * @param delay Delay, in milliseconds, or 0 to disable.
     */
    public void setDebounceDelay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Debounce delay must be positive.");
        }
        // Clear any pending events
        removeDebounceCallback();
        mDebounceDelay = delay;
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        removeDebounceCallback();
        mListener = null;

        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mInterruptCallback);
            try {
                mButtonGpio.close();
            } finally {
                mButtonGpio = null;
            }
        }
    }

    /**
     * Invoke button event callback
     */
    private void performButtonEvent(boolean state) {
        if (mListener != null) {
            mListener.onButtonEvent(this, state);
        }
    }

    /**
     * Clear pending debouce check
     */
    private void removeDebounceCallback() {
        if (mPendingCheckDebounce != null) {
            mDebounceHandler.removeCallbacks(mPendingCheckDebounce);
            mPendingCheckDebounce = null;
        }
    }

    /**
     * Pending check to delay input events from the initial
     * trigger edge.
     */
    private final class CheckDebounce implements Runnable {
        private boolean mTriggerState;

        public CheckDebounce(boolean triggerState) {
            mTriggerState = triggerState;
        }

        public boolean getTriggerState() {
            return mTriggerState;
        }

        @Override
        public void run() {
            if (mButtonGpio != null) {
                try {
                    // Final check that state hasn't changed
                    if (mButtonGpio.getValue() == mTriggerState) {
                        performButtonEvent(mTriggerState);
                    }
                    removeDebounceCallback();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to read button value", e);
                }
            }
        }
    }

    static class ButtonInputDriver {
        private static final String DRIVER_NAME = "Button";
        private static final int DRIVER_VERSION = 1;
        static InputDriver build(Button button, int keyCode) {
            InputDriver inputDriver = InputDriver.builder(InputDevice.SOURCE_CLASS_BUTTON)
                    .name(DRIVER_NAME)
                    .version(DRIVER_VERSION)
                    .keys(new int[]{keyCode})
                    .build();
            button.setOnButtonEventListener(new OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button b, boolean pressed) {
                    int keyAction = pressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
                    inputDriver.emit(new KeyEvent[]{
                            new KeyEvent(keyAction, keyCode)
                    });
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
