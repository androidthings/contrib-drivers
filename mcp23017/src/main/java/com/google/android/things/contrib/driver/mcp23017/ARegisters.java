/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.mcp23017;

/* package */ class ARegisters implements Registers {

    /* package */ static final int IODIR_A = 0x00;
    /* package */ static final int IPOL_A = 0x02;
    /* package */ static final int GPINTEN_A = 0x04;
    /* package */ static final int DEFVAL_A = 0x06;
    /* package */ static final int INTCON_A = 0x08;
    /* package */ static final int GPPU_A = 0x0C;
    /* package */ static final int INTF_A = 0x0E;
    /* package */ static final int GPIO_A = 0x12;

    @Override
    public int getIODIR() {
        return IODIR_A;
    }

    @Override
    public int getIPOL() {
        return IPOL_A;
    }

    @Override
    public int getGRIPTEN() {
        return GPINTEN_A;
    }

    @Override
    public int getDEFVAL() {
        return DEFVAL_A;
    }

    @Override
    public int getINTCON() {
        return INTCON_A;
    }

    @Override
    public int getGPPU() {
        return GPPU_A;
    }

    @Override
    public int getINTF() {
        return INTF_A;
    }

    @Override
    public int getGPIO() {
        return GPIO_A;
    }
}
