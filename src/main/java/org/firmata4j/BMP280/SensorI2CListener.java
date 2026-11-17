/* 2024-06-11 James Smith's BMP280 Example */

/* SensorI2CListener class */
//
package org.firmata4j.BMP280;

import org.firmata4j.I2CDevice;
import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorI2CListener implements I2CListener, Comparable<SensorI2CListener>{
    private static final Logger LOGGER = LoggerFactory.getLogger(SensorI2CListener.class);

    private final I2CDevice theI2CDevice;
    SensorI2CListener(I2CDevice theI2CDevice){
        this.theI2CDevice = theI2CDevice;
    }
    @Override
    public void onReceive(I2CEvent theI2CEvent) {

        byte[] data = theI2CEvent.getData();
        LOGGER.debug("Received BMP280 I2C response from {}", theI2CEvent.getDevice());
        LOGGER.atTrace().addArgument(() -> Arrays.toString(data)).log("BMP280 payload bytes: {}");
        if (data == null || data.length != 1) {
            LOGGER.warn("Expected one BMP280 ID byte, received {}", data == null ? 0 : data.length);
        } else {
            LOGGER.info("BMP280 chip ID: 0x{}", Integer.toHexString(data[0] & 0xFF));
        }
    }

    @Override
    public int compareTo( SensorI2CListener o) {
        return 0;
    }
}
