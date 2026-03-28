package org.firmata4j.SGP30;

import org.firmata4j.*;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;

import java.io.IOException;

public class SGP30Example {
    /**
     * Demonstrates how Firmata4j receives I2C data from a sensor.
     *
     * <p>The example creates a board connection and an {@link SGP30} wrapper.
     * The wrapper owns an {@code I2CListener} that handles inbound replies.
     * Calling {@code getAirQuality()} looks synchronous from the caller's point
     * of view, but under the hood the response still arrives through the
     * listener callback.</p>
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        String myUSBPort = "COM4";
        final IODevice arduinoBoard = new FirmataDevice(myUSBPort);

        arduinoBoard.start();
        arduinoBoard.ensureInitializationIsDone();
        System.out.println("Connection to device running firmata on: " + myUSBPort);

        byte sgp30I2Caddr = 0x58;
        final SGP30 sgp30 = new SGP30(arduinoBoard, sgp30I2Caddr);

        System.out.println("CO2 Sensor Object Created");
        int[] serialNumber = sgp30.getSerial();
        System.out.println("My Serial # " + serialNumber);

        while (true){
            Thread.sleep(2000);
            int[] airQuality = sgp30.getAirQuality();
            System.out.printf("eCO2 = %d ppm TVOC = %d \n", airQuality[0], airQuality[1]);
        }
    }
}
