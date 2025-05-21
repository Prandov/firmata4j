/* 2024-06-11 James Smith's BMP280 Example */

/* SensorI2CListener class */
//
package org.firmata4j.BMP280;

import org.firmata4j.I2CDevice;
import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;
import org.firmata4j.SGP30.SGP30Listener;

import java.math.BigInteger;
import java.util.Arrays;

public class SensorI2CListener implements I2CListener, Comparable<SensorI2CListener>{

    private final I2CDevice theI2CDevice;
    SensorI2CListener(I2CDevice theI2CDevice){
        this.theI2CDevice = theI2CDevice;
    }
    @Override
    public void onReceive(I2CEvent theI2CEvent) {

        // Print out the event details (address, data, etc.)
        System.out.println("Listener reports the following information: ");
        System.out.println("The event:" + theI2CEvent + ". Note: data is in base 10.");
        System.out.println("the Device address: " +theI2CEvent.getDevice());
        System.out.println("Event data array: " + Arrays.toString(theI2CEvent.getData()) + " in base 10.");
        /* convert the byte array and print it */
        String hexString = new BigInteger(1,theI2CEvent.getData()).toString(16);
        System.out.println("Event data array: 0x" + hexString);
        /* again, but compact */
        System.out.println("Event data array: 0x" + new BigInteger(1,theI2CEvent.getData()).toString(16));


    }

    @Override
    public int compareTo( SensorI2CListener o) {
        System.out.println("compareTo+++++++++++++++++++++++++++++++++++++++");
        return 0;
    }
}