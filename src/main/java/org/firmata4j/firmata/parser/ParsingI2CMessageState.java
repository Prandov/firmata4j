/* 
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Oleg Kurbatov (o.v.kurbatov@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.firmata4j.firmata.parser;

import org.firmata4j.fsm.AbstractState;
import org.firmata4j.fsm.Event;
import org.firmata4j.fsm.FiniteStateMachine;
import static org.firmata4j.firmata.parser.FirmataEventType.*;
import static org.firmata4j.firmata.parser.FirmataToken.*;

/**
 * This class parses inbound I2C messages and publishes them when they are
 * complete.
 *
 * @author William Reichardt
 */
public class ParsingI2CMessageState extends AbstractState {

    public ParsingI2CMessageState(FiniteStateMachine fsm) {
        super(fsm);
    }

    /*
     * /* I2C reply
     * -------------------------------
     * 0  START_SYSEX (0xF0) (MIDI System Exclusive)
     * 1  I2C_REPLY (0x77)
     * 2  slave address (LSB)
     * ... Lotsa Bytes
     * n  END_SYSEX (0xF7)
     */
    /**
     * Collects I2C message from individual bytes.
     *
     * @param b the input byte
     */
    @Override
    public void process(byte b) {
        if (b == END_SYSEX) {
            byte[] buffer = getBuffer();
            transitTo(WaitingForMessageState.class);
            if (buffer.length < 4 || buffer.length % 2 != 0) {
                Event error = new Event(ERROR_MESSAGE);
                error.setBodyItem(ERROR_DESCRIPTION, "Malformed I2C reply: expected address/register pairs and paired data bytes.");
                publish(error);
                return;
            }
            int address = decodePair(buffer, 0);
            if (address > 0x7F) {
                Event error = new Event(ERROR_MESSAGE);
                error.setBodyItem(ERROR_DESCRIPTION, "10-bit I2C addresses are not supported.");
                publish(error);
                return;
            }
            // 2.5 protocol audit: preserve unsigned register bits instead of narrowing to a signed byte.
            // Address/register are 14-bit fields; payload pairs represent 8-bit bytes.
            // For example, 0xD0 must stay 208 to match ask(0xD0, ...), not become -48.
            int register = decodePair(buffer, 2);
            byte[] message = new byte[(buffer.length - 4) / 2];
            for (int i = 0; i < message.length; i++) {
                message[i] = (byte) decodePair(buffer, 4 + i * 2);
            }
            Event event = new Event(I2C_MESSAGE);
            event.setBodyItem(I2C_ADDRESS, (byte) address);
            event.setBodyItem(I2C_REGISTER, register);
            event.setBodyItem(I2C_MESSAGE, message);
            publish(event);
        } else {
            bufferize(b);
        }
    }

    private static int decodePair(byte[] buffer, int offset) {
        return (buffer[offset] & 0x7F) | ((buffer[offset + 1] & 0x7F) << 7);
    }

}
