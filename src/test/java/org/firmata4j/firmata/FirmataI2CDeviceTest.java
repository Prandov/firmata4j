package org.firmata4j.firmata;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.firmata4j.I2CEvent;
import org.firmata4j.Parser;
import org.firmata4j.transport.TransportInterface;
import org.junit.Test;

import static org.junit.Assert.*;

/** Regression tests for callback routing; no serial port or sensors required. */
public class FirmataI2CDeviceTest {
    @Test
    public void registerlessReplyReachesPendingCallback() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        FirmataI2CDevice sensor = new FirmataI2CDevice(new FirmataDevice(transport), (byte) 0x58);
        AtomicReference<I2CEvent> received = new AtomicReference<>();

        sensor.ask((byte) 3, received::set);
        assertArrayEquals(FirmataMessageFactory.i2cReadRequest((byte) 0x58,
                FirmataI2CDevice.REGISTER_NOT_SET, 3, false), transport.writes.get(0));
        byte[] payload = {0, 34, 101};
        sensor.onReceive(0, payload);

        assertNotNull("Register 0 replies must reach register-less requests", received.get());
        assertSame(sensor, received.get().getDevice());
        assertArrayEquals(payload, received.get().getData());
        assertEquals("Event retains the register reported by firmware", 0, received.get().getRegister());
    }

    @Test
    public void registeredCallbackTakesPriorityOverRegisterlessCallback() throws Exception {
        FirmataI2CDevice sensor = new FirmataI2CDevice(
                new FirmataDevice(new RecordingTransport()), (byte) 0x77);
        AtomicInteger registered = new AtomicInteger();
        AtomicInteger registerless = new AtomicInteger();
        sensor.ask((byte) 1, event -> registerless.incrementAndGet());
        sensor.ask(0, (byte) 1, event -> registered.incrementAndGet());

        sensor.onReceive(0, new byte[]{88});
        assertEquals(1, registered.get());
        assertEquals(0, registerless.get());
        sensor.onReceive(0, new byte[]{89});
        assertEquals(1, registerless.get());
    }

    @Test
    public void oneShotCallbackRunsOnlyOnce() throws Exception {
        FirmataI2CDevice sensor = new FirmataI2CDevice(
                new FirmataDevice(new RecordingTransport()), (byte) 0x77);
        AtomicInteger calls = new AtomicInteger();
        sensor.ask(0xD0, (byte) 1, event -> calls.incrementAndGet());

        sensor.onReceive(0xD0, new byte[]{88});
        sensor.onReceive(0xD0, new byte[]{88});
        assertEquals(1, calls.get());
    }

    @Test
    public void pendingReadsAreSeparateForDifferentAddresses() throws Exception {
        FirmataDevice board = new FirmataDevice(new RecordingTransport());
        FirmataI2CDevice sgp30 = (FirmataI2CDevice) board.getI2CDevice((byte) 0x58);
        FirmataI2CDevice bmp280 = (FirmataI2CDevice) board.getI2CDevice((byte) 0x77);
        AtomicReference<I2CEvent> gasReply = new AtomicReference<>();
        AtomicReference<I2CEvent> pressureReply = new AtomicReference<>();
        sgp30.ask((byte) 3, gasReply::set);
        bmp280.ask((byte) 1, pressureReply::set);

        bmp280.onReceive(0, new byte[]{88});
        assertNull(gasReply.get());
        assertSame(bmp280, pressureReply.get().getDevice());
        sgp30.onReceive(0, new byte[]{0, 34, 101});
        assertSame(sgp30, gasReply.get().getDevice());
        assertArrayEquals(new byte[]{88}, pressureReply.get().getData());
    }

    private static final class RecordingTransport implements TransportInterface {
        private final List<byte[]> writes = new ArrayList<>();

        @Override public void start() { }
        @Override public void stop() { }
        @Override public void setParser(Parser parser) { }
        @Override public void write(byte[] bytes) { writes.add(bytes.clone()); }
    }
}
