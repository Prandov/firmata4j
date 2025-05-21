package org.firmata4j.SGP30;

import org.firmata4j.I2CDevice;


public class SGP30Sensor implements I2CDevice {
    private final FirmataDevice device;
    private final byte address = 0x58;

    public SGP30Sensor(FirmataDevice device) {
        this.device = device;
    }

    public void initialize() {
        device.startI2C(address);
        // Send init command
        device.sendI2CRequest(address, new byte[]{(byte)0x20, (byte)0x03});
    }

    public void readAirQuality() {
        device.sendI2CRequest(address, new byte[]{(byte)0x20, (byte)0x08});
    }

    public void addListener() {
        device.addI2CListener(event -> {
            byte[] data = event.getData();
            // Parse CO2 and TVOC from data
        });
    }
}
