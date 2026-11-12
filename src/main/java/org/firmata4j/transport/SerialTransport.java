/* 
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2018 Oleg Kurbatov (o.v.kurbatov@gmail.com)
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
package org.firmata4j.transport;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import org.firmata4j.Parser;

/**
 * Allows connections over the serial interface.
 *
 * @author Ali Kia
 */
public class SerialTransport implements TransportInterface {

    private static final int BAUD_RATE = 57600;
    private static final int DATA_BITS = 8;

    private Parser parser;

    private final SerialPort port;

    public SerialTransport(String portName) {
        this.port = SerialPort.getCommPort(portName);
    }

    @Override
    public void start() throws IOException {
        if (!port.isOpen()) {
            port.setComPortParameters(BAUD_RATE, DATA_BITS, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            if (port.openPort()) {
                port.addDataListener(new SerialPortDataListener() {
                    @Override
                    public int getListeningEvents() {
                        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
                    }

                    @Override
                    public void serialEvent(SerialPortEvent event) {
                        if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                            parser.parse(event.getReceivedData());
                        }
                    }
                });
            } else {
                throw new IOException("Cannot start firmata device: port=" + port);
            }
        }
    }

    @Override
    public void stop() throws IOException {
        if (port.isOpen()) {
            port.removeDataListener();
            if (!port.closePort()) {
                throw new IOException("Cannot properly stop firmata device: port=" + port);
            }
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        int written = port.writeBytes(bytes, bytes.length);
        if (written != bytes.length) {
            throw new IOException("Cannot send message to device: port=" + port);
        }
    }

    @Override
    public void setParser(Parser parser) {
        this.parser = parser;
    }

}
