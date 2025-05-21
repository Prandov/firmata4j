/* 2024-06-11 James Smith's BMP280 Example */

/* SensorI2CListener class */
//
package org.firmata4j.SGP30;

import org.firmata4j.I2CDevice;
import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;
import java.math.BigInteger;
import java.util.Arrays;

// Suggested to use a single listener in your Firmata4j setup to manage data from all sensors

public class SGP30Listener implements I2CListener, Comparable<SGP30Listener>{
//public class SGP30Listener implements I2CListener{

    //private final I2CDevice theI2CDevice;

    private final SGP30 sgp30;

    //private byte[] receivedData;
    //SGP30Listener(I2CDevice theI2CDevice, SGP30 sgp30){
    SGP30Listener(SGP30 sgp30){
//        this.theI2CDevice = theI2CDevice;
        this.sgp30 = sgp30;
    }
    @Override
    public void onReceive(I2CEvent theI2CEvent) {
        byte[] receivedData = theI2CEvent.getData(); //2025EP
        // Check if receivedData is not empty
        if (receivedData != null && receivedData.length > 0) {
            sgp30.i2cReceivedData = receivedData;
            // Copilot said to use a setter functions
            //sgp30.setReceivedData(data);
            //sgp30.setNewDataAvailable(true);

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
//sending received data to processor instead
//    public byte[] getReceivedData() {
//        return receivedData;
//    }

    @Override
    public int compareTo(SGP30Listener o) {
        System.out.println("compareTo+++++++++++++++++++++++++++++++++++++++");
        return 0;
    }
}

//package org.firmata4j.SGP30;
//
//import org.firmata4j.I2CDevice;
//import org.firmata4j.I2CEvent;
//import org.firmata4j.I2CListener;
//import java.math.BigInteger;
//import java.util.Arrays;
//
//public class SGP30Listener implements I2CListener {
//
//    // final I2CDevice theI2CDevice;
//    private I2CDevice theI2CDevice;
//    //private final SGP30 sgp30;
//    SGP30Listener(I2CDevice theI2CDevice){
//        this.theI2CDevice = theI2CDevice;
//    }
//    //SGP30Listener(SGP30 sgp30,I2CDevice theI2CDevice){
//        //this.sgp30 = sgp30;
//        //this.theI2CDevice = theI2CDevice;
//    //}
//    @Override
//    public byte[] onReceive(I2CEvent theI2CEvent) {
//
//        // Print out the event details (address, data, etc.)
//        System.out.println("Listener reports the following information: ");
//        System.out.println("The event:" + theI2CEvent + ". Note: data is in base 10.");
//        System.out.println("the Device address: " +theI2CEvent.getDevice());
//
//        byte[] data = theI2CEvent.getData(); //2025EP
//
//        //SGP30.processData(data);
//
//        System.out.println("Event data array: " + data + " in base 10.");
//        /* convert the byte array and print it */
//        String hexString = new BigInteger(1,theI2CEvent.getData()).toString(16);
//        System.out.println("Event data array: 0x" + hexString);
//        /* again, but compact */
//        System.out.println("Event data array: 0x" + new BigInteger(1,theI2CEvent.getData()).toString(16));
//
//        //sgp30.newDataAvailable = true;
//        //return new byte[0];
//        return data;
//
//
//
//    }
//}
