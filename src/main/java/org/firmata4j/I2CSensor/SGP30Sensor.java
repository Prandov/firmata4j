package org.firmata4j.I2CSensor;

//3. Sensor Implementations
//        Each sensor class implements I2CSensorDevice:

import org.firmata4j.firmata.FirmataDevice;

public class SGP30Sensor extends I2CSensorDeviceAbstract {

        public SGP30Sensor(FirmataDevice device) {
            super(device, (byte) 0x58);
            I2CSensorManager.getInstance(device).registerSensor(this);
        }

        @Override
        public void initialize() {
            // Send "Init Air Quality" command
            write((byte) 0x20, (byte) 0x03);
        }

        @Override
        public void readSensor() {
            // Send "Measure Air Quality" command
            write((byte) 0x20, (byte) 0x08);
            // Request 6 bytes of data
            read(6);
        }

        @Override
        public void handleResponse(byte[] data) {
            if (data.length < 6) {
                System.err.println("Invalid data length");
                return;
            }

            int co2 = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            int tvoc = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);

            System.out.println("eCO2: " + co2 + " ppm, TVOC: " + tvoc + " ppb");
        }
    }
