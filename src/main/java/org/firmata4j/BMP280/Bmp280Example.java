/* 2024-06-11 James Smith's BMP280 Example */

/* Main class Example */

package org.firmata4j.BMP280;
import org.firmata4j.*;
import org.firmata4j.I2CDevice;
import org.firmata4j.firmata.FirmataDevice;

import java.io.IOException;

public class Bmp280Example {

    //static String USBPORT = "/dev/cu.usbserial-0001"; // TO-DO : modify based on your computer setup.
    static String USBPORT = "COM4";

    public static void main(String[] args) throws IOException, InterruptedException {
        //var myUSBPort = USBPORT; //James uses Var
        String myUSBPort = USBPORT; //I2C Example uses String

        //var groveArduinoBoard = new FirmataDevice(myUSBPort);  //James uses Var
        final IODevice groveArduinoBoard = new FirmataDevice(myUSBPort);  //I2C Example uses final IODevice

        groveArduinoBoard.start();
        groveArduinoBoard.ensureInitializationIsDone();

        // Set up the BMP280 sensor & a listener for I2C events.
        I2CDevice firmataToI2CPressureSensor = groveArduinoBoard.getI2CDevice(Bmp280Token.BMP280_ADDR);     // Sensor as I2C Object.
        SensorI2CListener  myI2CListener  = new SensorI2CListener(firmataToI2CPressureSensor);  // Listener setup.
        firmataToI2CPressureSensor.subscribe(myI2CListener);                                    // Subscribe to listener
        Bmp280 groveBmp280Sensor = new Bmp280(firmataToI2CPressureSensor, myI2CListener);       // BMP280 specifics

        // Initialize the sensor.  Ask it for its ID.
        groveBmp280Sensor.init();  // init() method is like the SSD1306 Method.

        // Shut off connection to the board.
        groveArduinoBoard.stop();
    }
}
