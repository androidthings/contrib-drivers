package com.google.android.things.contrib.driver.matrixkeypad;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import java.io.IOException;

public class MatrixKeypad implements AutoCloseable {
    private static final String TAG = MatrixKeypad.class.getSimpleName();
    private static final int SOFTWAREPOLL_DELAY_MS = 16;

    private MatrixKey[][] mMatrix;
    private Gpio[] mGpioRows;
    private Gpio[] mGpioCols;
    private int[] mKeyCodes;

    private OnKeyEventListener mKeyEventListener;

    private Handler mKeyScanHandler;
    private Runnable mKeyScanRunnable = new Runnable() {
        @Override
        public void run() {
            if (mKeyEventListener != null) {
                try {
                    // Provide voltage to each column line separately, keeping the others at HIGH-Z
                    //   as inputs.
                    for (int c = 0; c < mGpioCols.length; c++) {
                        Gpio colPin = mGpioCols[c];
                        colPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                        for (int r = 0; r < mGpioRows.length; r++) {
                            Gpio rowPin = mGpioRows[r];
                            if (rowPin.getValue() && !mMatrix[r][c].isPressed()) {
                                mMatrix[r][c].setPressed(true);
                                keyDown(mMatrix[r][c]);
                            } else if (!rowPin.getValue() && mMatrix[r][c].isPressed()) {
                                mMatrix[r][c].setPressed(false);
                                keyUp(mMatrix[r][c]);
                            }
                        }
                        colPin.setDirection(Gpio.DIRECTION_IN);
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            mKeyScanHandler.postDelayed(this, SOFTWAREPOLL_DELAY_MS);
        }
    };

    /**
     * Creates a new driver for your matrix keypad.
     *
     * @param rowPins The string names for each pin related to rows, from row 1 - row M.
     * @param colPins The string names for each pin related to cols, from col 1 - col N.
     * @param keyCodes The integer keycodes for each key, from top-left to bottom-right.
     * @throws IOException If there's a problem connecting to any pin.
     */
    public MatrixKeypad(String[] rowPins, String[] colPins, int[] keyCodes) throws IOException {
        this(rowPins, colPins, keyCodes, null);
    }

    public MatrixKeypad(String[] rowPins, String[] colPins, int[] keyCodes,
            Handler handler) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        mGpioRows = new Gpio[rowPins.length];
        mGpioCols = new Gpio[colPins.length];
        mKeyCodes = keyCodes;
        mMatrix = new MatrixKey[rowPins.length][colPins.length];

        // Initialize Gpio and keys
        for (int r = 0; r < rowPins.length; r++) {
            mGpioRows[r] = pioService.openGpio(rowPins[r]);
            mGpioRows[r].setDirection(Gpio.DIRECTION_IN);
            for (int c = 0; c < colPins.length; c++) {
                if (mGpioCols[c] == null) {
                    mGpioCols[c] = pioService.openGpio(colPins[c]);
                    mGpioCols[c].setDirection(Gpio.DIRECTION_IN);
                }
                mMatrix[r][c] = new MatrixKey(keyCodes[r * colPins.length + c]);
            }
        }

        initKeyScanHandler(handler);
    }

    /* package */ MatrixKeypad(Gpio[] rowGpio, Gpio[] colGpio, int[] keyCodes) throws IOException {
        this(rowGpio, colGpio, keyCodes, null);
    }

    /* package */ MatrixKeypad(Gpio[] rowGpio, Gpio[] colGpio, int[] keyCodes, Handler handler)
            throws IOException {
        mGpioRows = rowGpio;
        mGpioCols = colGpio;
        mKeyCodes = keyCodes;

        mMatrix = new MatrixKey[rowGpio.length][colGpio.length];

        // Initialize Gpio and keys
        for (int r = 0; r < rowGpio.length; r++) {
            mGpioRows[r].setDirection(Gpio.DIRECTION_IN);
            for (int c = 0; c < colGpio.length; c++) {
                if (mGpioCols[c] == null) {
                    mGpioCols[c].setDirection(Gpio.DIRECTION_IN);
                }
                mMatrix[r][c] = new MatrixKey(keyCodes[r * colGpio.length + c]);
            }
        }

        mKeyScanHandler = handler;
        initKeyScanHandler(handler);
    }

    /**
     *
     * @param callback A callback which is run when a button is pressed or released.
     */
    public void setKeyCallback(OnKeyEventListener callback) {
        mKeyEventListener = callback;
    }

    private void initKeyScanHandler(Handler handler) {
        if (handler == null) {
            mKeyScanHandler = new Handler(Looper.myLooper());
        } else if (mKeyScanHandler == null) {
            mKeyScanHandler = new Handler(handler.getLooper());
        }
        mKeyScanHandler.post(mKeyScanRunnable);
    }

    /**
     * Emits a key down event through the input driver
     *
     * @param keycode keycode to send
     */
    /* package */ void keyDown(int keycode) {
        if (mKeyEventListener != null) {
            MatrixKey matrixKey = new MatrixKey(keycode);
            matrixKey.setPressed(true);
            mKeyEventListener.onKeyEvent(matrixKey);
        } else {
            Log.w(TAG, "Key down event occurred, but nothing is listening.");
        }
    }

    /**
     * Emits a key up event through the input driver
     *
     * @param keycode keycode to send
     */
    /* package */ void keyUp(int keycode) {
        if (mKeyEventListener != null) {
            // Pressed will be false by default.
            MatrixKey matrixKey = new MatrixKey(keycode);
            mKeyEventListener.onKeyEvent(matrixKey);
        } else {
            Log.w(TAG, "Key up event occurred, but nothing is listening.");
        }
    }

    /**
     * Emits a key down event through the input driver
     *
     * @param matrixKey key that was pressed
     */
    /* package */ void keyDown(MatrixKey matrixKey) {
        if (mKeyEventListener != null) {
            mKeyEventListener.onKeyEvent(matrixKey);
        } else {
            Log.w(TAG, "Key down event occurred, but nothing is listening.");
        }
    }

    /**
     * Emits a key up event through the input driver
     *
     * @param matrixKey key that was unpressed
     */
    /* package */ void keyUp(MatrixKey matrixKey) {
        if (mKeyEventListener != null) {
            mKeyEventListener.onKeyEvent(matrixKey);
        } else {
            Log.w(TAG, "Key up event occurred, but nothing is listening.");
        }
    }

    /**
     * Closes all of the GPIO pins used by the keypad.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        mKeyScanHandler.removeCallbacks(mKeyScanRunnable);
        for (Gpio gpio : mGpioRows) {
            gpio.close();
        }
        mGpioRows = null;
        for (Gpio gpio : mGpioCols) {
            gpio.close();
        }
        mGpioCols = null;
    }

    public interface OnKeyEventListener {
        void onKeyEvent(MatrixKey matrixKey);
    }
}
