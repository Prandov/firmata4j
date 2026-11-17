package org.firmata4j.firmata.parser;

import java.util.ArrayList;
import java.util.List;
import org.firmata4j.fsm.Event;
import org.firmata4j.fsm.FiniteStateMachine;
import org.junit.Test;

import static org.firmata4j.firmata.parser.FirmataEventType.*;
import static org.junit.Assert.*;

public class Protocol25ParserTest {
    @Test
    public void initialAnalogAndDigitalReportsUseExistingInputMessages() {
        // Protocol 2.4+ sends these immediately when reporting is enabled.
        List<Event> events = parse((byte) 0xE2, (byte) 0x7F, (byte) 0x7F);
        assertEquals(ANALOG_MESSAGE_RESPONSE, events.get(0).getType());
        assertEquals(2, events.get(0).getBodyItem(PIN_ID));
        assertEquals(16383, events.get(0).getBodyItem(PIN_VALUE));
        events = parse((byte) 0x91, (byte) 0, (byte) 1);
        assertEquals(8, events.size());
        assertEquals(15, events.get(7).getBodyItem(PIN_ID));
        assertEquals(1, events.get(7).getBodyItem(PIN_VALUE));
    }

    @Test
    public void versionReportHasMajorAndMinorOnly() {
        List<Event> events = parse((byte) 0xF9, (byte) 2, (byte) 5);
        assertEquals(1, events.size());
        assertEquals(PROTOCOL_MESSAGE, events.get(0).getType());
        assertEquals(2, events.get(0).getBodyItem(PROTOCOL_MAJOR));
        assertEquals(5, events.get(0).getBodyItem(PROTOCOL_MINOR));
    }

    @Test
    public void i2cRegistersAreUnsignedAndPayloadKeepsHighBits() {
        List<Event> events = parse((byte) 0xF0, (byte) 0x77, (byte) 0x77, (byte) 0,
                (byte) 0x50, (byte) 1, (byte) 0x7F, (byte) 1, (byte) 0xF7);
        assertEquals(0xD0, events.get(0).getBodyItem(I2C_REGISTER));
        assertEquals((byte) 0x77, events.get(0).getBodyItem(I2C_ADDRESS));
        assertArrayEquals(new byte[]{(byte) 0xFF}, (byte[]) events.get(0).getBodyItem(I2C_MESSAGE));
        events = parse((byte) 0xF0, (byte) 0x77, (byte) 0x58, (byte) 0,
                (byte) 0x34, (byte) 0x24, (byte) 0, (byte) 0, (byte) 0xF7);
        assertEquals(0x1234, events.get(0).getBodyItem(I2C_REGISTER));
    }

    @Test
    public void malformedI2cReplyDoesNotPreventNextVersionReport() {
        List<Event> events = parse((byte) 0xF0, (byte) 0x77, (byte) 0x58, (byte) 0,
                (byte) 0, (byte) 0, (byte) 1, (byte) 0xF7, (byte) 0xF9, (byte) 2, (byte) 5);
        assertEquals(ERROR_MESSAGE, events.get(0).getType());
        assertEquals(PROTOCOL_MESSAGE, events.get(1).getType());
    }

    @Test
    public void capabilityResponsePreservesPullupMode() {
        List<Event> events = parse((byte) 0xF0, (byte) 0x6C, (byte) 0, (byte) 1,
                (byte) 11, (byte) 1, (byte) 127, (byte) 0xF7);
        assertArrayEquals(new byte[]{0, 11}, (byte[]) events.get(0).getBodyItem(PIN_SUPPORTED_MODES));
        assertEquals(PIN_CAPABILITIES_FINISHED, events.get(1).getType());
    }

    private static List<Event> parse(byte... bytes) {
        List<Event> events = new ArrayList<>();
        FiniteStateMachine fsm = new FiniteStateMachine(WaitingForMessageState.class) {
            @Override public void handle(Event event) { events.add(event); }
        };
        for (byte value : bytes) {
            fsm.process(value);
        }
        return events;
    }
}
