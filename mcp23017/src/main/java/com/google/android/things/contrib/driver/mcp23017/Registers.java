package com.google.android.things.contrib.driver.mcp23017;

public interface Registers {

    int getIODIR();

    int getIPOL();

    int getGRIPTEN();

    int getDEFVAL();

    int getINTCON();

    int getGPPU();

    int getINTF();

    int getGPIO();
}
