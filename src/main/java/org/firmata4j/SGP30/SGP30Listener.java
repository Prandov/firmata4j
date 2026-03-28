/* 2024-06-11 James Smith's BMP280 Example */

/* SensorI2CListener class */
//
package org.firmata4j.SGP30;

import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;
import java.math.BigInteger;
import java.util.Arrays;

public class SGP30Listener implements I2CListener, Comparable<SGP30Listener>{
    private final SGP30 sgp30;

    SGP30Listener(SGP30 sgp30){
        this.sgp30 = sgp30;
    }

    /**
     * Receives asynchronous Firmata I2C replies for the owning {@link SGP30}.
     *
     * <p>The event contains the raw bytes read from the sensor's I2C address.
     * This listener forwards that payload to the sensor-specific parser so the
     * transport layer stays generic while the {@code SGP30} class remains
     * responsible for decoding its own protocol.</p>
     */
    @Override
    public void onReceive(I2CEvent theI2CEvent) {
        byte[] receivedData = theI2CEvent.getData(); //2025EP
        // Check if receivedData is not empty
        if (receivedData != null && receivedData.length > 0) {
            sgp30.i2cReceivedData = receivedData;

            // Print out the event details (address, data, etc.)
            System.out.println("Listener reports the following information: ");
            System.out.println("-The Device:      " + theI2CEvent.getDevice());
            System.out.println("-The Event:       " + theI2CEvent + ". Note: data is in base 10.");
            System.out.println("-The Data Bytes:  " + Arrays.toString(receivedData));
            /* convert the byte array and print it */
            System.out.println("-The Data in hex: 0x" + new BigInteger(1, theI2CEvent.getData()).toString(16));

            sgp30.processData(receivedData);
        }
    }

    @Override
    public int compareTo(SGP30Listener o) {
        return Integer.compare(System.identityHashCode(this), System.identityHashCode(o));
    }
}
