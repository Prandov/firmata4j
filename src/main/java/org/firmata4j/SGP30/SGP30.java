package org.firmata4j.SGP30;

import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.firmata4j.IODevice;

import org.firmata4j.I2CDevice;
import org.slf4j.Logger; //2025JS
import org.slf4j.LoggerFactory; //2025JS
import java.io.IOException; //2025JS
import java.util.Arrays; //2025JS

/**
    SGP30 CO2 Sensor
    I2CAddress: 0x58
    I2C Commands:
    _________________________________________________________________________________________________________
    1. Init_air_quality = 0x2003;      // No Response                // ExecutionTime: Typ.2ms   Max.10ms
    2. Measure_air_quality = 0x2008;   // Reply: 6 bytes inc. CRC    // ExecutionTime: Typ.10ms  Max.12ms
    3. Get_iaqBaseline = 0x2015;       // Reply: 6 bytes inc. CRC    // ExecutionTime: Typ.1ms   Max.10ms
    4. Set_iaqBaseline = 0x201E;       // Send: 6 bytes inc. CRC     // ExecutionTime: Typ.1ms   Max.10ms
    5. Set_absHumidity = 0x2061;       // Send 3 bytes inc. CRC      // ExecutionTime: Typ.1ms   Max.10ms
    6. Measure_test = 0x2032;          // Reply: 3 bytes inc. CRC.   // ExecutionTime: Typ.200ms Max.220ms On chip self test returns 0xD400
    7. Get_featureSet = 0x202F;        // Reply: 3 bytes inc. CRC    // ExecutionTime: Typ.1ms   Max.10ms
    8. Measure_rawSignals = 0x2050;    // Reply: 6 bytes inc. CRC    // ExecutionTime: Typ.20ms  Max.25ms
    9. Get_tvocInceptiveBaseline = 0x20b3 // Reply: 3 bytes inc. CRC // ExecutionTime: Typ.1ms   Max.10ms
    10.Set_tvocBaseline = 0x2077       // Send 3 bytes inc. CRC      // ExecutionTime: Typ.1ms   Max.10ms
    11.Get_serial_ID = 0x3682;         // Reply: 9 bytes inc. CRC    // ExecutionTime: Typ.10ms  Max.12ms
    _________________________________________________________________________________________________________
*/

public class SGP30 {
    private I2CDevice sgp30Device; //25-03-21
    private SGP30Listener sgp30Listener; //25-03-21
    protected byte[] i2cReceivedData;
    volatile boolean newDataAvailable = false;
    int[] receivedSensorData;

    private static final Logger LOGGER = LoggerFactory.getLogger(SGP30.class); //Final
    private static final int[] possibleFeatureSets = new int[]{0x0020, 0x0022};
    private byte address;
    private int wordLen;
    private int busNo;

    private int crc8Init;

    public final int[] serial;

    private int crc8Polynomial;
    public final int[] featureSet;
    private volatile int[] lastIaqMeasurement;

    /**
     * Default constructor with default values for address, word and bus
     * abc.SGP30 sensor will almost always have 0x58 as address and a wordLength of 2
     * busNo might be different depending on GPIO pins used
     *
     * @throws IOException                              could not read from abc.SGP30
     * @throws InterruptedException                     interrupted during delay
     * //OLD @throws I2CFactory.UnsupportedBusNumberException wrong bus no
     */
    public SGP30(IODevice arduinoObj, byte address) throws IOException, InterruptedException {
        // assign object attributes
        this.address = 0x58;
        this.wordLen = 2;
        this.busNo = 0; //Arduino default bus should be 0
        this.crc8Init = 0xFF;
        this.crc8Polynomial = 0x31;

        sgp30Device = arduinoObj.getI2CDevice(address);
        sgp30Listener = new SGP30Listener(this);

        this.serial = loadSerial();
        Thread.sleep(50);
        this.featureSet = loadFeatureset();
        Thread.sleep(50);

        if (IntStream.of(possibleFeatureSets).noneMatch(x -> x == featureSet[0])) {
            throw new RuntimeException("Unknown featureset");
        }
        iaqInit();
    }
    /**
     * Gets the current TVOC value
     *
     * @return the TVOC value
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public int getTVOC() throws IOException, InterruptedException {
        System.out.println("Method#2: getTVOC()");
        return getAirQuality()[1];
    }

    /**
     * Gets the currently active TVOC Baseline
     *
     * @return the TVOC baseline
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public int getBaseLineTVOC() throws IOException, InterruptedException {
        System.out.println("Method#3: getBaseLineTVOC()");
        return getIaqBaseLine()[1];
    }

    /**
     * Gets the current eCO2 value
     *
     * @return the eCO2 value
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public int getECO2() throws IOException, InterruptedException {
        System.out.println("Method#4: getECO2()");
        return getAirQuality()[0];
    }

    public int[] getAirQuality() throws IOException, InterruptedException {
        int[] measurement = iaqMeasure();
        lastIaqMeasurement = measurement;
        return Arrays.copyOf(measurement, measurement.length);
    }

    public int[] getLastAirQuality() {
        if (lastIaqMeasurement == null) {
            return null;
        }
        return Arrays.copyOf(lastIaqMeasurement, lastIaqMeasurement.length);
    }

    /**
     * Gets the currently active eCO2 Baseline
     *
     * @return the eCO2 baseline
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public int getBaseLineECO2() throws IOException, InterruptedException {
        System.out.println("Method 5#: getBaseLineECO2()");
        return getIaqBaseLine()[0];
    }

    public int[] getSerial() {
        System.out.println("Method 6#: getSerial()");
        return serial;
    }
    /**
     *
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    private void iaqInit() throws IOException, InterruptedException {
        System.out.println("Command #1. Init_air_quality");
// 1. Init_air_quality = 0x2003;      // No Response                // ExecutionTime: Typ.2ms   Max.10ms

        int command = 0x2003;
        int typDelay = 2; // Typ.2ms
        int maxDelay = 10; // Max.10ms
        byte replySz = 0; // No Response
        sensorCommandSendOrReceive(command, maxDelay, (byte) 0);
    }
    /**
     * Queries for the current values of eCO2 and TVOC
     *
     * @return the current values of eCO2 and TVOC
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    private int[] iaqMeasure() throws IOException, InterruptedException {
        System.out.println("Command #2. Measure_air_quality");
// 2. Measure_air_quality = 0x2008;   // Reply: 6 bytes inc. CRC    // ExecutionTime: Typ.10ms  Max.12ms
        int command = 0x2008;
        int typDelay = 10; // Typ.10ms
        int maxDelay = 12; // Max.12ms
        byte replySz = 6; // 6 bytes
        return sensorCommandSendOrReceive(command, maxDelay, replySz);
    }
    /**
     * Queries for the currently active baseline values
     *
     * @return the current baselines
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    private int[] getIaqBaseLine() throws IOException, InterruptedException {
        System.out.println("Command #3. Get_iaqBaseline");
//3. Get_iaqBaseline = 0x2015;       // Reply: 6 bytes inc. CRC    // ExecutionTime: Typ.1ms   Max.10ms
        int command = 0x2015;
        int typDelay = 1; // Typ.1ms
        int maxDelay = 10; // Max.10ms
        byte replySz = 6; // 6 bytes
        return sensorCommandSendOrReceive(command, maxDelay, replySz);
    }
    /**
     * Sets the previously IAQ algorithm baseline for eCO2 and TVOC
     *
     * @param eCO2 eCO2 value that has to act as baseline
     * @param TVOC TVOC value that has to act as baseline
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public void setIaqBaseline(int eCO2, int TVOC) throws IOException, InterruptedException {
// 4. Set_iaqBaseline = 0x201E;       // Send: 6 bytes inc. CRC     // ExecutionTime: Typ.1ms   Max.10ms
        System.out.println("Command #4. Set_iaqBaseline()");
        if (eCO2 == 0 || TVOC == 0) {
            throw new RuntimeException("Invalid baseline values");
        }
        int commandID = 0x201E;
        int typDelay = 1; // Typ.1ms
        int maxDelay = 10; // Max.10ms
        int replySize = 0;
        byte[] bytesToSend = new byte[6];
    // For Loop Approach
        int[] values = {eCO2, TVOC};
        int count = 0;
        for (int value : values) {
            bytesToSend[count * 3] = (byte) (value >> 8);
            bytesToSend[count * 3 + 1] = (byte) (value & 0xFF);
            bytesToSend[count * 3 + 2] = (byte) generateCrc(new int[]{value >> 8, value & 0xFF});
            count++;
        }
    //Direct Assignment Approach
        bytesToSend[0] = (byte) (eCO2 >> 8);
        bytesToSend[1] = (byte) (eCO2 & 0xFF);
        bytesToSend[2] = (byte) generateCrc(new int[]{eCO2 >> 8, eCO2 & 0xFF});
        bytesToSend[3] = (byte) (TVOC >> 8);
        bytesToSend[4] = (byte) (TVOC & 0xFF);
        bytesToSend[5] = (byte) generateCrc(new int[]{TVOC >> 8, TVOC & 0xFF});

        sensorCommandSendOrReceive(commandID, maxDelay, (byte) replySize, bytesToSend);
    }
    /**
     * Sets the humidity in g/m3 for eCO2 and TVOC compensation algorithm
     *
     * @param gramsPM3 g/m3 of humidity for compensation
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public void setIaqHumidity(int gramsPM3) throws IOException, InterruptedException {
// 5. Set_absHumidity = 0x2061;       // Send 3 bytes inc. CRC      // ExecutionTime: Typ.1ms   Max.10ms
        int command = 0x2061;
        int typDelay = 1; // Typ.1ms
        int maxDelay = 10; // Max.10ms
        int replySize = 0;
        int tmp = gramsPM3 * 256;
        byte[] bytesToSend = new byte[3];
        bytesToSend[0] = (byte) (tmp >> 8);
        bytesToSend[1] = (byte) (tmp & 0xFF);
        bytesToSend[2] = (byte) generateCrc(new int[]{tmp >> 8, tmp & 0xFF});
        sensorCommandSendOrReceive(command, maxDelay, (byte) replySize, bytesToSend);
    }
    /**
     * On chip self test
     *
     * @return the read result
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public int[] measureTest() throws IOException, InterruptedException {
//6. Measure_test = 0x2032;          // Reply: 3 bytes inc. CRC.   // ExecutionTime: Typ.200ms Max.220ms On chip self test returns 0xD400
        int command = 0x2032;
        int typDelay = 200; // Typ.20ms
        int maxDelay = 220; // Max.220ms
        byte replySz = 3; // 3 bytes
        return sensorCommandSendOrReceive(command, maxDelay, replySz);
    }
    /**
     * Loads the Featureset register of the SGP30 and compares it with known valid Featuresets of the device
     *
     * @return
     * @throws IOException          could not read from SGP30
     * @throws InterruptedException interrupted during delay
     */
    private int[] loadFeatureset() throws IOException, InterruptedException {
// 7. Get_featureSet = 0x202F;        // Reply: 3 bytes inc. CRC    // ExecutionTime: Typ.1ms   Max.10ms
        System.out.println("Method#3: loadFeatureset()");
        int command = 0x202F;
        int typDelay = 1; // Typ.1ms
        int maxDelay = 10; // Max.10ms
        byte replySz = 3; // 3 bytes
        return sensorCommandSendOrReceive(command, maxDelay, replySz);
    }
    /**
     * Gets the Raw Values
     *
     * @return
     * @throws IOException          could not read from SGP30
     * @throws InterruptedException interrupted during delay
     */
    private int[] measureRaw() throws IOException, InterruptedException {
// 8. Measure_rawSignals = 0x2050;    // Reply: 6 bytes inc. CRC    // ExecutionTime: Typ.20ms  Max.25ms
        System.out.println("Method#8: measureRaw()");
        int command = 0x2050;
        int typDelay = 20; // Typ.20ms
        int maxDelay = 25; // Max.25ms
        byte replySz = 6; // 6 bytes
        return sensorCommandSendOrReceive(command, maxDelay, replySz);
    }
    /**
     * Queries for the currently active baseline values
     *
     * @return the current TVOC Inceptive baselines
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    private int[] getTvocInceptiveBaseline() throws IOException, InterruptedException {
        System.out.println("Method 9#: getTvocInceptiveBaseline()");
// 9. Get_tvocInceptiveBaseline = 0x20b3 // Reply: 3 bytes inc. CRC // ExecutionTime: Typ.1ms   Max.10ms
        int command = 0x20B3;
        int typDelay = 1; // Typ.1ms
        int maxDelay = 10; // Max.10ms
        byte replySz = 3; // 3 bytes
        return sensorCommandSendOrReceive(command, maxDelay,replySz);
    }
    /**
     * Sets the TVOC Baseline
     *
     * @param PPM
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    public void setTvocBaseline(int PPM) throws IOException, InterruptedException {
// 10.Set_tvocBaseline = 0x2077       // Send 3 bytes inc. CRC      // ExecutionTime: Typ.1ms   Max.10ms
        int command = 0x2077;
        int typDelay = 1; // Typ.1ms
        int maxDelay = 10; //Max.10ms
        byte replySize = 0;
        int tmp = PPM * 256;
        byte[] bytesToSend = new byte[3];
        bytesToSend[0] = (byte) (tmp >> 8);
        bytesToSend[1] = (byte) (tmp & 0xFF);
        bytesToSend[2] = (byte) generateCrc(new int[]{tmp >> 8, tmp & 0xFF});
        sensorCommandSendOrReceive(command, maxDelay, replySize,bytesToSend);
    }

    /**
     * Loads the serial no. of the SGP30 device
     *
     * @throws IOException          could not read from SGP30
     * @throws InterruptedException interrupted during delay
     */
    private int[] loadSerial() throws IOException, InterruptedException {
// 11.Get_serial_ID = 0x3682;         // Reply: 9 bytes inc. CRC    // ExecutionTime: Typ.10ms  Max.12ms
        System.out.println("Method#11: loadSerial()");
        //byte[] commandID = new byte[]{0x36, (byte) 0x82};
        int command = 0x3682;
        int typDelay = 10; // Typ.10ms
        int maxDelay = 12; // Max.12ms
        byte replySz = 9;  // 9 bytes
        return sensorCommandSendOrReceive(command, maxDelay, replySz);
    }
    /**
     * @param command   command to execute over I2C
     * @param maxDelay  delay to wait for read of I2C
     * @param replySize size of the expected reply
     * @return the read result
     * @throws IOException          could not read from abc.SGP30
     * @throws InterruptedException interrupted during delay
     */
    /**
     * Sends a single SGP30 command and optionally waits for a reply.
     *
     * <p>Firmata I2C reads are asynchronous. This method writes the command with
     * {@code tell(...)}, waits for the sensor's execution time, then calls
     * {@code ask(...)} to request the reply. The response does not come back as
     * the return value of {@code ask(...)}. Instead, Firmata delivers the bytes
     * later through {@code I2CListener.onReceive()}, and the listener stores the
     * parsed result on this {@code SGP30} instance.</p>
     *
     * <p>This helper wraps that event-driven flow in a blocking method so the
     * example can expose a simple sensor API.</p>
     */
    private int[] sensorCommandSendOrReceive(int command, int maxDelay, byte replySize, byte... bytesToSend) throws IOException, InterruptedException {
        // A. Clear new data flag
        newDataAvailable = false;
        receivedSensorData = null;
        // B. send the command to the device
            byte[] commandBytes = new byte[] {
                (byte) ((command >> 8) & 0xFF),
                (byte) (command & 0xFF)
            };

            System.out.println("I2C 'tell' Command");
            sgp30Device.tell(commandBytes);
            if (bytesToSend.length > 0) {
                sgp30Device.tell(bytesToSend);
            }
            if (replySize > 0) {    //Required because iaqInit has no reply
                Thread.sleep(maxDelay);
                System.out.println("I2C 'ask' Command");
                sgp30Device.ask(replySize,sgp30Listener);
        // C. Wait for data back, maybe check the new data flag every millisecond
                int i = 0;
                while (!newDataAvailable && i < maxDelay) {
                    Thread.sleep(1);
                    i++;
                }
                if (!newDataAvailable || receivedSensorData == null) {
                    throw new IOException(
                            String.format("Sensor command 0x%04X timed out after %d ms waiting for I2C data", command, maxDelay),
                            new TimeoutException("No I2C response received")
                    );
                }
            } else {
                System.out.println("No reply expected for this Command");
            }

            //Thread.sleep(10); //Stuff was out of order on the console so this hopefully slows it down
        // D. get the data and process it.
            return receivedSensorData;
    }



    public void setNewDataAvailable(boolean newDataAvailable) {
        this.newDataAvailable = newDataAvailable;
    }
    public void setReceivedData(byte[] data) {
        this.i2cReceivedData = data;
    }

    /**
     * Decodes the raw SGP30 payload delivered by the I2C listener.
     *
     * <p>Each SGP30 word is returned as two data bytes followed by a CRC byte:
     * {@code [MSB, LSB, CRC]}. This method validates the CRC for each word,
     * converts the payload into integer sensor values, and publishes the parsed
     * result back to the waiting request method.</p>
     */
    void processData(byte[] crcResponse) {
        System.out.println("Process the Received Data");
        System.out.println("-Response bytes with CRC byte: " + Arrays.toString(crcResponse));
            int replyLength = crcResponse.length;
            int[] intResponse = new int[replyLength];
            //In Java, bytes are signed convert to int to avoid confusion
            for (int i = 0; i < replyLength; i++) {
                intResponse[i] = crcResponse[i] & 0xFF;
            }
            int[] intResult = new int[replyLength/3];
            byte[] byteResult = new byte[replyLength/3*2];
            for (int i = 0; i < replyLength; i += 3) { //Increment by 3 each time (2 data + 1 crc byte)
                int[] word = new int[]{intResponse[i], intResponse[i + 1]};
                int crc = intResponse[i + 2];
                int crcCheck = generateCrc(word);
                if (crcCheck != crc) {
                    System.out.println("-Response CRC Error");
                   //throw new IOException("CRC error " + crc + " != " + crcCheck + " for crc check " + i);
                }
                else {
                    System.out.printf("-Response CRC byte:     %d Pass!%n", i+3);
                    intResult[i / 3] = (word[0] << 8 | word[1]);
                    byteResult[i / 3 * 2] = (byte) (word[0] & 0xFF);
                    byteResult[i / 3 * 2 + 1] = (byte) (word[1] & 0xFF);
                }
            }
        System.out.println("-Response integer data: " + Arrays.toString(intResult));
        System.out.println("-Response byte data:    " + Arrays.toString(byteResult));

        // Convert byte array to hex string
        String hexString = bytesToHex(byteResult);
        System.out.println("Response in Hex: 0x" + hexString);
        receivedSensorData = intResult;
        newDataAvailable = true; // Set the newDataAvailable flag to true
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Generates CRC8 for given data
     *
     * @param data int array to generate CRC8 for
     * @return CRC8 of given data
     */
    private int generateCrc(int[] data) {
        int crc = crc8Init;//0xFF;
        for (int bt : data) {

            crc ^= bt;
            for (int i = 0; i < 8; i++) {
                int test = crc & 0x80;
                if (test != 0) {
                    crc = (crc << 1) ^ crc8Polynomial; //0x31
                } else {
                    crc <<= 1;
                }
            }
        }
        return crc & 0xFF;
    }
} //End_of SGP30 Class
