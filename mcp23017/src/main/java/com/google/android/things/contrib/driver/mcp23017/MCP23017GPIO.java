package com.google.android.things.contrib.driver.mcp23017;

import static com.google.android.things.contrib.driver.mcp23017.Registers.REGISTER_GPIO_A;
import static com.google.android.things.contrib.driver.mcp23017.Registers.REGISTER_GPIO_B;

public enum MCP23017GPIO {

    A0(1, "GPA0", REGISTER_GPIO_A),
    A1(2, "GPA1", REGISTER_GPIO_A),
    A2(4, "GPA2", REGISTER_GPIO_A),
    A3(8, "GPA3", REGISTER_GPIO_A),
    A4(16, "GPA4", REGISTER_GPIO_A),
    A5(32, "GPA5", REGISTER_GPIO_A),
    A6(64, "GPA6", REGISTER_GPIO_A),
    A7(128, "GPA7", REGISTER_GPIO_A),
    B0(1, "GPB0", REGISTER_GPIO_B),
    B1(2, "GPB1", REGISTER_GPIO_B),
    B2(4, "GPB2", REGISTER_GPIO_B),
    B3(8, "GPB3", REGISTER_GPIO_B),
    B4(16, "GPB4", REGISTER_GPIO_B),
    B5(32, "GPB5", REGISTER_GPIO_B),
    B6(64, "GPB6", REGISTER_GPIO_B),
    B7(128, "GPB7", REGISTER_GPIO_B);


    private int address;
    private String name;
    private int register;

    MCP23017GPIO(int address, String name, int register) {
        this.address = address;
        this.name = name;
        this.register = register;
    }

    public int getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public int getRegister() {
        return register;
    }
}
