package org.firmata4j.firmata;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.firmata4j.Parser;
import org.firmata4j.Pin;
import org.firmata4j.transport.TransportInterface;
import org.junit.Test;

import static org.firmata4j.firmata.parser.FirmataToken.*;
import static org.junit.Assert.*;

/** Wire fixtures from the Firmata core and I2C protocol specifications. */
public class FirmataProtocol25Test {
    @Test
    public void reportedProtocolControlsSinglePinWriteSupport() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        FirmataDevice board = new FirmataDevice(transport);
        LinkedBlockingQueue<org.firmata4j.fsm.Event> versions = new LinkedBlockingQueue<>();
        board.addProtocolMessageHandler(org.firmata4j.firmata.parser.FirmataEventType.PROTOCOL_MESSAGE,
                new org.firmata4j.Consumer<org.firmata4j.fsm.Event>() {
                    @Override public void accept(org.firmata4j.fsm.Event event) { versions.add(event); }
                });
        assertFalse(board.supportsSinglePinWrite());
        try {
            board.start();
            assertArrayEquals(new byte[]{(byte) 0xF9}, transport.writes.get(0));
            for (byte minor : new byte[]{3, 4, 5}) {
                transport.parser.parse(new byte[]{(byte) 0xF9, 2, minor});
                assertNotNull("Protocol reply must be handled", versions.poll(2, TimeUnit.SECONDS));
                assertEquals(minor >= 5, board.supportsSinglePinWrite());
            }
        } finally {
            board.stop();
        }
    }

    @Test
    public void singlePinAndLegacyPortWritesHaveDistinctEncodings() {
        assertArrayEquals(new byte[]{(byte) 0xF5, 13, 1},
                FirmataMessageFactory.setDigitalPinValue((byte) 13, true));
        assertArrayEquals(new byte[]{(byte) 0xF5, 13, 0},
                FirmataMessageFactory.setDigitalPinValue((byte) 13, false));
        assertArrayEquals(new byte[]{(byte) 0x91, 0, 1},
                FirmataMessageFactory.setDigitalPinValue((byte) 1, (byte) 0x80));
    }

    @Test
    public void pinModesMatchProtocolAndUnknownModesStayUnsupported() {
        assertArrayEquals(new byte[]{(byte) 0xF4, 2, 11},
                FirmataMessageFactory.setMode((byte) 2, Pin.Mode.PULLUP));
        assertArrayEquals(new byte[]{(byte) 0xF4, 2, 127},
                FirmataMessageFactory.setMode((byte) 2, Pin.Mode.IGNORED));
        assertSame(Pin.Mode.PULLUP, Pin.Mode.resolve((byte) 11));
        assertSame(Pin.Mode.IGNORED, Pin.Mode.resolve((byte) 127));
        for (byte mode : new byte[]{12, 13, 14, 126, -1}) {
            assertSame(Pin.Mode.UNSUPPORTED, Pin.Mode.resolve(mode));
        }
    }

    @Test
    public void i2cConfigEncodesAllFourteenDelayBits() {
        for (int delay : new int[]{0, 127, 128, 255, 256, 12000, 16383}) {
            byte[] message = FirmataMessageFactory.i2cConfigRequest(delay);
            assertEquals(delay, (message[3] << 7) | message[2]);
        }
        assertThrows(IllegalArgumentException.class, () -> FirmataMessageFactory.i2cConfigRequest(16384));
        assertThrows(IllegalArgumentException.class, () -> FirmataMessageFactory.i2cConfigRequest(-1));
    }

    @Test
    public void repeatedStartPreservesReadOnceAndContinuousModeBits() {
        assertArrayEquals(new byte[]{(byte) 0xF0, 0x76, 0x77, 0x48, 0x50, 1, 1, 0, (byte) 0xF7},
                FirmataMessageFactory.i2cReadRequest((byte) 0x77, 0xD0, 1, false, true));
        assertEquals(0x50, FirmataMessageFactory.i2cReadRequest((byte) 0x77, 0xD0, 1, true, true)[3]);
        assertEquals(0x08, FirmataMessageFactory.i2cReadRequest((byte) 0x77, 0xD0, 1, false)[3]);
    }

    @Test
    public void deviceCarriesRestartFlagAndUnsignedLengthIntoRequests() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        FirmataI2CDevice sensor = new FirmataI2CDevice(new FirmataDevice(transport), (byte) 0x77);
        sensor.setReadRestart(true);
        sensor.ask(0xD0, (byte) 200, event -> { });
        byte[] message = transport.last();
        assertEquals(0x48, message[3]);
        assertEquals(200, (message[7] << 7) | message[6]);
        sensor.startReceivingUpdates(0xD0, (byte) 1);
        assertEquals(0x50, transport.last()[3]);
        sensor.stopReceivingUpdates();
        assertEquals(0x18, transport.last()[3]);
        sensor.setReadRestart(false);
        sensor.ask(0xD0, (byte) 1, event -> { });
        assertEquals(0x08, transport.last()[3]);
    }

    @Test
    public void outputPinUsesDirectWriteWhenProtocolSupportsIt() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        TestDevice board = new TestDevice(transport, true);
        FirmataPin pin = board.output(13);
        pin.setValue(9);
        assertArrayEquals(new byte[]{(byte) 0xF5, 13, 1}, transport.last());
        assertEquals(1, pin.getValue());
        pin.setValue(1);
        assertEquals("Unchanged output should not send another write", 1, transport.writes.size());
    }

    @Test
    public void olderProtocolUsesPortWriteIncludingPartialFinalPort() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        TestDevice board = new TestDevice(transport, false);
        board.output(0);
        board.output(1);
        FirmataPin pin = board.output(2);
        pin.setValue(1);
        assertArrayEquals(new byte[]{(byte) 0x90, 4, 0}, transport.last());
    }

    private static final class TestDevice extends FirmataDevice {
        private final boolean directWrites;
        private final List<FirmataPin> pins = new ArrayList<>();

        TestDevice(RecordingTransport transport, boolean directWrites) {
            super(transport);
            this.directWrites = directWrites;
        }

        FirmataPin output(int id) {
            FirmataPin pin = new FirmataPin(this, (byte) id);
            pin.initMode(Pin.Mode.OUTPUT);
            pins.add(pin);
            return pin;
        }

        @Override boolean supportsSinglePinWrite() { return directWrites; }
        @Override public int getPinsCount() { return pins.size(); }
        @Override public Pin getPin(int index) { return pins.get(index); }
    }

    private static final class RecordingTransport implements TransportInterface {
        private Parser parser;
        final List<byte[]> writes = new ArrayList<>();
        byte[] last() { return writes.get(writes.size() - 1); }
        @Override public void start() { }
        @Override public void stop() { }
        @Override public void setParser(Parser parser) { this.parser = parser; }
        @Override public void write(byte[] bytes) { writes.add(bytes.clone()); }
    }
}
