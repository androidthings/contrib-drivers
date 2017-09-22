package com.google.android.things.contrib.driver.output;

import android.os.Handler;
import android.util.Log;
import android.view.ViewConfiguration;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;


public class Output implements AutoCloseable {
    private static final String TAG = Output.class.getSimpleName();

    private Handler mDelayHadler;
    private Handler mTimeOutHadler;
    private Handler mRepetionHadler;
    private OnOutputEventListener mListener;
    private boolean mState = true;
    private long mDebounceDelay = ViewConfiguration.getTapTimeout();

    private Gpio mOutputGpio;

    /**
     * Create a new Output for the given GPIO pin name.
     * @param pin GPIO pin where the button is attached.
     * @throws IOException
     */
    public Output(String pin) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        mOutputGpio = pioService.openGpio(pin);
        mDelayHadler = new Handler();
        mTimeOutHadler = new Handler();
        mRepetionHadler = new Handler();

        try {
            mOutputGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException|RuntimeException e) {
            close();
            throw e;
        }
    }


    /**
     * Set the time delay after an edge trigger that the output
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
            throw new IllegalArgumentException("Debounce delay cannot be negative.");
        }
        // Clear any pending events
        removeCallbacks();
        mDebounceDelay = delay;
    }


    /*
     *  Turn On or Off Led
     *  @param boolean
     */
    public void turn(boolean value){

        if (mOutputGpio != null) {
            try {
                mOutputGpio.setValue(value);
            } catch (IOException e) {
                Log.w(TAG, "Could not set outout", e);
            }
        }
    }

    /*
    *  Turn On and Off Led
    *  Blink led
    */
    public void toggle(){
        turn(true);
        mDelayHadler.postDelayed(new Runnable() {
            @Override
            public void run() {
                turn(false);
            }
        },mDebounceDelay);
    }


   /*
   *  Turn repetion
   *  Blink led emit repetion
   */
    public void toggleRepeat(){
        mDelayHadler.postDelayed(postRepetion,mDebounceDelay);
    }


    /*
     * call repetion emit  repetion to listerner
     */
    private Runnable postRepetion = new Runnable() {

        @Override
        public void run() {
            turn(mState);
            mState = !mState;
            mListener.onOutputEvent(true);
            mListener.onOutputEvent(false);
            mDelayHadler.postDelayed(this,mDebounceDelay);
        }
    };

    /*
     *  emit event when time finish
     */
    public void toggleTimeOut(int timeout){
        toggle();
        mTimeOutHadler.postDelayed(postTimeout,timeout);
    }

    /*
     * call to timeout
     */
    private Runnable postTimeout = new Runnable() {
        @Override
        public void run() {
            if (mListener != null) {
                mListener.onOutputEvent(true);
                mListener.onOutputEvent(false);
            }
        }
    };

    /*
     * remove call repetion
     * Cancel repetion
     */
    public void removeCallbackRepetion(){
        try {
            mRepetionHadler.removeCallbacks(postRepetion);
        }catch (Exception e) {
            Log.e(TAG,"Error remove callbacks");
        }
    }

   /*
   * remove call time
   * Cancel time
   */
    public void removeCallbackTimeout(){
        try {
            mTimeOutHadler.removeCallbacks(postTimeout);
        }catch (Exception e) {
            Log.e(TAG,"Error remove callbacks");
        }
    }

   /*
   * remove all calls
   */
    public void removeCallbacks(){
       removeCallbackRepetion();
       removeCallbackTimeout();
    }

    @Override
    public void close() throws IOException {

        removeCallbacks();

        if (mOutputGpio != null) {
            try {
                mOutputGpio.close();
            } finally {
                mOutputGpio = null;
            }
        }
    }

    /**
     * Set the listener to be called when a output event occurred.
     *
     * @param listener button event listener to be invoked.
     */
    public void setOnOutputEventListener(OnOutputEventListener listener) {
        mListener = listener;
    }

    public interface OnOutputEventListener {

        void onOutputEvent(boolean pressed);
    }

}
