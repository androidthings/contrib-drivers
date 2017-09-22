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

    public void setDebounceDelay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Debounce delay cannot be negative.");
        }
        // Clear any pending events
        removeCallbacks();
        mDebounceDelay = delay;
    }


    public void turn(boolean value){

        if (mOutputGpio != null) {
            try {
                mOutputGpio.setValue(value);
            } catch (IOException e) {
                Log.w(TAG, "Could not set outout", e);
            }
        }
    }

    public void toggle(){
        turn(true);
        mDelayHadler.postDelayed(new Runnable() {
            @Override
            public void run() {
                turn(false);
            }
        },mDebounceDelay);
    }


    public void toggleRepeat(){
        mDelayHadler.postDelayed(postRepetion,mDebounceDelay);
    }


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

    public void toggleTimeOut(int timeout){
        toggle();
        mTimeOutHadler.postDelayed(postTimeout,timeout);
    }

    private Runnable postTimeout = new Runnable() {
        @Override
        public void run() {
            if (mListener != null) {
                mListener.onOutputEvent(true);
                mListener.onOutputEvent(false);
            }
        }
    };



    public void removeCallbackRepetion(){
        try {
            mRepetionHadler.removeCallbacks(postRepetion);
        }catch (Exception e) {
            Log.e(TAG,"Error remove callbacks");
        }
    }

    public void removeCallbackTimeout(){
        try {
            mTimeOutHadler.removeCallbacks(postTimeout);
        }catch (Exception e) {
            Log.e(TAG,"Error remove callbacks");
        }
    }


    public void removeCallbacks(){
        try {
            mTimeOutHadler.removeCallbacks(postTimeout);
            mRepetionHadler.removeCallbacks(postRepetion);
        }catch (Exception e) {
            Log.e(TAG,"Error remove callbacks");
        }
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

    public void setOnOutputEventListener(OnOutputEventListener listener) {
        mListener = listener;
    }

    public interface OnOutputEventListener {

        void onOutputEvent(boolean pressed);
    }

}
    