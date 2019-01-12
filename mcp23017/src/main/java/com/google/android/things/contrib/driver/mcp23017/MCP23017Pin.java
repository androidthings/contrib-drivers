package com.google.android.things.contrib.driver.mcp23017;

import com.google.android.things.pio.Gpio;

public interface MCP23017Pin extends Gpio {

    int getAddress();

    Registers getRegisters();
}
