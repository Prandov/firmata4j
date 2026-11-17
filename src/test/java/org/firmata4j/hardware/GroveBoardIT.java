package org.firmata4j.hardware;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.firmata4j.I2CDevice;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.fsm.Event;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.firmata4j.firmata.parser.FirmataEventType.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Opt-in tests for the Grove kit, StandardFirmata 2.5.7 / protocol 2.5. */
public class GroveBoardIT {
    private static FirmataDevice board;

    @BeforeClass
    public static void connect() throws Exception {
        // Also protects against opening a port accidentally via an IDE test run.
        assumeTrue("Enable the hardware-tests Maven profile", Boolean.getBoolean("firmata.hardware"));
        board = new FirmataDevice(System.getProperty("firmata.port", "COM5"));
        try {
            board.start();
            board.ensureInitializationIsDone(); // Library bounds initialization to 15 seconds.
        } catch (Exception failure) {
            try {
                board.stop();
            } catch (Exception cleanup) {
                failure.addSuppressed(cleanup);
            }
            board = null;
            throw failure;
        }
    }

    @AfterClass
    public static void disconnect() throws Exception {
        if (board != null) {
            board.stop();
            board = null;
        }
    }

    @Test(timeout = 5000)
    public void boardInitializesAndReportsProtocol25() throws Exception {
        assertTrue(board.isReady());
        CompletableFuture<Event> version = new CompletableFuture<>();
        board.addProtocolMessageHandler(PROTOCOL_MESSAGE, new org.firmata4j.Consumer<Event>() {
            @Override
            public void accept(Event event) {
                version.complete(event);
            }
        });
        board.sendMessage((byte) 0xF9); // REPORT_VERSION: major/minor only, no patch field.
        Event reply = version.get(2, TimeUnit.SECONDS);
        assertEquals(2, reply.getBodyItem(PROTOCOL_MAJOR));
        assertEquals(5, reply.getBodyItem(PROTOCOL_MINOR));
        System.out.println("Connected firmware: " + board.getProtocol());
    }

    @Test(timeout = 5000)
    public void bmp280ReturnsExpectedChipId() throws Exception {
        assertBmp280Id();
    }

    @Test(timeout = 45000)
    public void sgp30KeepsRespondingAlongsideBmp280() throws Exception {
        assumeTrue("Connect SGP30 and pass -Dfirmata.sgp30=true", Boolean.getBoolean("firmata.sgp30"));
        I2CDevice gas = board.getI2CDevice((byte) 0x58);
        byte[] serial = command(gas, 0x3682, 12, 9);
        assertCrcWords(serial);
        byte[] features = command(gas, 0x202F, 10, 3);
        assertCrcWords(features);
        int featureSet = word(features, 0);
        assertTrue("Unexpected SGP30 feature set: " + featureSet,
                featureSet == 0x0020 || featureSet == 0x0022);

        gas.tell((byte) 0x20, (byte) 0x03); // IAQ init has no response.
        Thread.sleep(20);
        long nextMeasurement = System.nanoTime();
        // Exceed the original nine-reading failure and exercise both addresses.
        for (int i = 0; i < 16; i++) {
            long wait = nextMeasurement - System.nanoTime();
            if (wait > 0) {
                TimeUnit.NANOSECONDS.sleep(wait);
            }
            long started = System.nanoTime();
            byte[] sample = command(gas, 0x2008, 12, 6);
            assertCrcWords(sample);
            assertTrue("eCO2 must be at least the sensor's 400 ppm floor", word(sample, 0) >= 400);
            System.out.printf("SGP30 sample %d: eCO2=%d ppm, TVOC=%d ppb%n",
                    i + 1, word(sample, 0), word(sample, 3));
            assertBmp280Id();
            nextMeasurement = started + TimeUnit.SECONDS.toNanos(1);
        }
    }

    private static void assertBmp280Id() throws Exception {
        I2CDevice pressure = board.getI2CDevice((byte) 0x77);
        pressure.tell((byte) 0xD0);
        byte[] reply = read(pressure, 1);
        assertEquals("BMP280 at 0x77 must return chip ID 0x58", 0x58, reply[0] & 0xFF);
    }

    private static byte[] command(I2CDevice device, int command, int executionMs, int length)
            throws Exception {
        device.tell((byte) (command >> 8), (byte) command);
        Thread.sleep(executionMs);
        return read(device, length);
    }

    private static byte[] read(I2CDevice device, int length) throws Exception {
        CompletableFuture<byte[]> reply = new CompletableFuture<>();
        device.ask((byte) length, event -> reply.complete(event.getData()));
        byte[] data = reply.get(2, TimeUnit.SECONDS);
        assertEquals("Unexpected payload length from " + device, length, data.length);
        return data;
    }

    private static int word(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static void assertCrcWords(byte[] data) {
        assertEquals(0, data.length % 3);
        for (int offset = 0; offset < data.length; offset += 3) {
            int crc = 0xFF;
            for (int i = offset; i < offset + 2; i++) {
                crc ^= data[i] & 0xFF;
                for (int bit = 0; bit < 8; bit++) {
                    crc = ((crc & 0x80) != 0 ? (crc << 1) ^ 0x31 : crc << 1) & 0xFF;
                }
            }
            assertEquals("SGP30 CRC mismatch in word " + offset / 3, data[offset + 2] & 0xFF, crc);
        }
    }
}
