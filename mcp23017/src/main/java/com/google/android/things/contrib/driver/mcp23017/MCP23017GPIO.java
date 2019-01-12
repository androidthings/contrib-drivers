package com.google.android.things.contrib.driver.mcp23017;

public enum MCP23017GPIO {

    A0(1, "GPA0", new ARegisters()),
    A1(2, "GPA1", new ARegisters()),
    A2(4, "GPA2", new ARegisters()),
    A3(8, "GPA3", new ARegisters()),
    A4(16, "GPA4", new ARegisters()),
    A5(32, "GPA5", new ARegisters()),
    A6(64, "GPA6", new ARegisters()),
    A7(128, "GPA7", new ARegisters()),
    B0(1, "GPB0", new BRegisters()),
    B1(2, "GPB1", new BRegisters()),
    B2(4, "GPB2", new BRegisters()),
    B3(8, "GPB3", new BRegisters()),
    B4(16, "GPB4", new BRegisters()),
    B5(32, "GPB5", new BRegisters()),
    B6(64, "GPB6", new BRegisters()),
    B7(128, "GPB7", new BRegisters());


    private int address;
    private String name;
    private Registers register;

    MCP23017GPIO(int address, String name, Registers register) {
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

    public Registers getRegisters() {
        return register;
    }
}
