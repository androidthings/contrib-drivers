package com.google.brillo.driver.captouch;

import android.content.Context;
import android.hardware.userdriver.InputDriver;
import android.hardware.userdriver.InputDriverEvent;
import android.hardware.userdriver.UserDriverManager;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.system.ErrnoException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * User-space driver to process capacitive touch events from the
 * CAP12xx family of touch controllers and forward them to the
 * Android input framework.
 */
@SuppressWarnings("WeakerAccess")
public class Cap12xxInputDriver implements AutoCloseable {
    private static final String TAG = "Cap12xxInputDriver";

    // Driver parameters
    private static final String DRIVER_NAME = "Cap12xx";
    private static final int DRIVER_VERSION = 1;
    // Input event states
    private static final int EVENT_RELEASED = 0;
    private static final int EVENT_PRESSED  = 1;
    // Constants defined in linux/input.h
    private static final int EV_SYN = 0;
    private static final int EV_KEY = 1;
    private static Integer[] SUPPORTED_EVENT_TYPE = {EV_SYN, EV_KEY};
    // Constants defined in linux/uinput.h
    private static final int UI_SET_EVBIT  = 0x40045564;
    private static final int UI_SET_KEYBIT = 0x40045565;

    private Context mContext;
    private Cap12xx mPeripheralDevice;
    // Framework input driver
    private InputDriver mInputDriver;
    // Key codes mapped to input channels
    private Integer[] mKeycodes;

    /**
     * Create a new Cap12xxInputDriver to forward capacitive touch events
     * to the Android input framework.
     *
     * @param context Current context, used for loading resources.
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @param keyCodes event codes to be emitted for each input channel.
     *                 Length must match the input channel count of the
     *                 touch controller.
     */
    public Cap12xxInputDriver(Context context,
                              String i2cName,
                              String alertName,
                              Cap12xx.Configuration chip,
                              int[] keyCodes) throws ErrnoException {
        this(context, i2cName, alertName, chip, null, keyCodes);
    }

    /**
     * Create a new Cap12xxInputDriver to forward capacitive touch events
     * to the Android input framework.
     *
     * @param context Current context, used for loading resources.
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @param handler optional {@link Handler} for software polling and callback events.
     * @param keyCodes event codes to be emitted for each input channel.
     *                 Length must match the input channel count of the
     *                 touch controller.
     */
    public Cap12xxInputDriver(Context context,
                              String i2cName,
                              String alertName,
                              Cap12xx.Configuration chip,
                              Handler handler,
                              int[] keyCodes) throws ErrnoException {
        Cap12xx peripheral = new Cap12xx(context, i2cName, alertName, chip, handler);
        init(context, peripheral, keyCodes);
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ Cap12xxInputDriver(Context context,
                                   Cap12xx peripheral,
                                   int[] keyCodes) throws ErrnoException {
        init(context, peripheral, keyCodes);
    }

    /**
     * Initialize peripheral defaults from the constructor.
     */
    private void init(Context context, Cap12xx peripheral, int[] keyCodes) {
        // Verify inputs
        if (keyCodes == null) {
            throw new IllegalArgumentException("Must provide a valid set of key codes.");
        }

        mKeycodes = new Integer[keyCodes.length];
        for (int i = 0; i < keyCodes.length; i++) {
            mKeycodes[i] = keyCodes[i];
        }

        mContext = context.getApplicationContext();
        mPeripheralDevice = peripheral;
        mPeripheralDevice.setOnCapTouchListener(mTouchListener);
    }

    /**
     * Set the repeat rate of generated events on active input channels.
     * Can be one of {@link Cap12xx#REPEAT_NORMAL}, {@link Cap12xx#REPEAT_FAST}, or
     * {@link Cap12xx#REPEAT_SLOW} to generate continuous events while any of
     * the input channels are actively detecting touch input.
     *
     * <p>Use {@link Cap12xx#REPEAT_DISABLE} to generate a single event for each
     * input touch and release. The default is {@link Cap12xx#REPEAT_NORMAL}.
     *
     * @param rate one of {@link Cap12xx#REPEAT_NORMAL}, {@link Cap12xx#REPEAT_SLOW},
     *             {@link Cap12xx#REPEAT_FAST}, or {@link Cap12xx#REPEAT_DISABLE}.
     *
     * @throws ErrnoException
     */
    public void setRepeatRate(@Cap12xx.RepeatRate int rate) throws ErrnoException {
        if (mPeripheralDevice != null) {
            mPeripheralDevice.setRepeatRate(rate);
        }
    }

    /**
     * Set the detection sensitivity of the capacitive input channels.
     *
     * <p>The default is {@link Cap12xx#SENSITIVITY_NORMAL}.
     *
     * @param sensitivity one of {@link Cap12xx#SENSITIVITY_NORMAL},
     *                    {@link Cap12xx#SENSITIVITY_LOW}, or {@link Cap12xx#SENSITIVITY_HIGH}.
     *
     * @throws ErrnoException
     */
    public void setSensitivity(@Cap12xx.Sensitivity int sensitivity) throws ErrnoException {
        if (mPeripheralDevice != null) {
            mPeripheralDevice.setSensitivity(sensitivity);
        }
    }

    /**
     * Set the maximum number of input channels allowed to simultaneously
     * generate active events. Additional touches beyond this number will
     * be ignored.
     *
     * @param count Maximum number of inputs allowed to generate events.
     *
     * @throws ErrnoException
     */
    public void setMultitouchInputMax(int count) throws ErrnoException {
        if (mPeripheralDevice != null) {
            mPeripheralDevice.setMultitouchInputMax(count);
        }
    }

    /**
     * Callback invoked when touch events are received from
     * the peripheral device.
     */
    private Cap12xx.OnCapTouchListener mTouchListener = new Cap12xx.OnCapTouchListener() {
        @Override
        public void onCapTouchEvent(Cap12xx controller, boolean[] inputStatus) {
            emitInputEvents(inputStatus);
        }
    };

    /**
     * Emit input events through the registered driver to the
     * Android input framework using the defined set of key codes.
     *
     * @param status Bitmask of input channel status flags.
     */
    private void emitInputEvents(boolean[] status) {
        if (mInputDriver == null) {
            Log.w(TAG, "Driver not yet registered");
            return;
        }

        // Emit an event for each defined input channel
        for (int i = 0; i < mKeycodes.length; i++) {
            boolean active = status[i];
            int code = mKeycodes[i];

            mInputDriver.emit(new InputDriverEvent[]{
                    new InputDriverEvent(EV_KEY, code, active ? EVENT_PRESSED : EVENT_RELEASED),
                    new InputDriverEvent(EV_SYN, 0, 0)
            });
        }
    }

    /**
     * Register this driver with the Android input framework.
     */
    public void register() {
        if (mInputDriver == null) {
            UserDriverManager manager = UserDriverManager.getManager();
            mInputDriver = buildInputDriver();
            manager.registerInputDriver(mInputDriver);
        }
    }

    /**
     * Unregister this driver with the Android input framework.
     */
    public void unregister() {
        if (mInputDriver != null) {
            UserDriverManager manager = UserDriverManager.getManager();
            manager.unregisterInputDriver(mInputDriver);
            mInputDriver = null;
        }
    }

    /**
     * Returns the {@link InputDriver} instance this touch controller
     * uses to emit input events based on the driver's event codes list.
     */
    private InputDriver buildInputDriver() {
        Map<Integer, Integer[]> supportedEvents = new HashMap<>();
        supportedEvents.put(UI_SET_EVBIT, SUPPORTED_EVENT_TYPE);
        supportedEvents.put(UI_SET_KEYBIT, mKeycodes);

        return InputDriver.builder(supportedEvents)
                .name(DRIVER_NAME)
                .version(DRIVER_VERSION)
                .build();
    }

    /**
     * Close this driver and any underlying resources associated with the connection.
     */
    @Override
    public void close() {
        unregister();

        if (mPeripheralDevice != null) {
            mPeripheralDevice.setOnCapTouchListener(null);
            mPeripheralDevice.close();
        }
    }
}
