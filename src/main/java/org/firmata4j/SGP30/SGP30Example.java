package org.firmata4j.SGP30;

import org.firmata4j.*;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;

import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SGP30Example {
    private static final Logger LOGGER = LoggerFactory.getLogger(SGP30Example.class);
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
        String myUSBPort = "COM5";
        final IODevice arduinoBoard = new FirmataDevice(myUSBPort);

        arduinoBoard.start();
        arduinoBoard.ensureInitializationIsDone();
        LOGGER.info("Connected to Firmata on {}", myUSBPort);

        byte sgp30I2Caddr = 0x58;
        final SGP30 sgp30 = new SGP30(arduinoBoard, sgp30I2Caddr);

        LOGGER.info("SGP30 initialized");
        int[] serialNumber = sgp30.getSerial();
        LOGGER.info("SGP30 serial words: {}", Arrays.toString(serialNumber));

        while (true){
            Thread.sleep(2000);
            int[] airQuality = sgp30.getAirQuality();
            LOGGER.info("eCO2 = {} ppm, TVOC = {} ppb", airQuality[0], airQuality[1]);
        }
    }
}
