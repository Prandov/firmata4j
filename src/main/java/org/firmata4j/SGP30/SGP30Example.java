package org.firmata4j.SGP30;
import org.firmata4j.SGP30.SGP30Listener;
import org.firmata4j.*;
import org.firmata4j.I2CDevice;
import org.firmata4j.IODevice;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.firmata.FirmataI2CDevice;

import java.io.IOException;

public class SGP30Example {
//OLD   public static void main(String[] args) throws InterruptedException, IOException, I2CFactory.UnsupportedBusNumberException {

     //static String USBPORT = "COM4";

        public static void main(String[] args) throws InterruptedException, IOException {

            String myUSBPort = "COM4"; // "/dev/cu.usbserial-0001"; // TO-DO : modify based on your computer setup.

            //var myUSBPort = USBPORT; //James uses Var
            //String myUSBPort = USBPORT; //I2C Example uses String

            //var arduinoBoard = new FirmataDevice(myUSBPort); //2025JS //error java: cannot find symbol, symbol: class var
            final IODevice arduinoBoard = new FirmataDevice(myUSBPort); //try this instead

            arduinoBoard.start();
            arduinoBoard.ensureInitializationIsDone();
            System.out.println("Connection to device running firmata on: " + myUSBPort);

            //I2CDevice i2cDevice = arduinoBoard.getI2CDevice((byte) 0x58);
            //System.out.println("SGP30 I2C Address set to: " + String.format("%x", i2cDevice.getAddress()));

            // Create an instance of the SGP30 class
            byte sgp30I2Caddr = 0x58;
            final SGP30 sgp30 = new SGP30(arduinoBoard, sgp30I2Caddr);

            System.out.println("CO2 Sensor Object Created");

            // Initialize the sensor.  Ask it for its ID.
            //sgp30co2.init();  // init() method is like the SSD1306 Method.

            //Get Serial number
            int[] serialNumber = sgp30.getSerial();
            System.out.println("My Serial # " + serialNumber);

        while (true){
            Thread.sleep(2000);
            System.out.printf("eCO2 = %d ppm TVOC = %d \n", sgp30.getECO2(), sgp30.getTVOC());
        }
    }

}
