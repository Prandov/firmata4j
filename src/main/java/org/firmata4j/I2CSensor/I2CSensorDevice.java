package org.firmata4j.I2CSensor;

import org.firmata4j.*;
//1. Define a Common Interface
//        Create an interface like I2CSensorDevice that all your sensor classes will implement.

public interface I2CSensorDevice extends I2CDevice {
    byte getAddress();
    void initialize();
    void readData();
    void handleI2CResponse(byte[] data);
}
