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

package org.firmata4j;

import java.io.IOException;

/**
 * Represents an I2C device and encapsulates logic to communicate to it.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public interface I2CDevice {

    /**
     * Returns address of I2C device.
     */
    byte getAddress();

    /**
     * Sets the firmware delay between selecting a register and reading from it.
     *
     * <p>The value is in microseconds, from 0 to 16383. For example,
     * {@code sensor.setDelay(128)} requests a 128-microsecond delay inside a
     * registered {@code ask(...)} operation. This is not a Java response timeout
     * and does not insert a delay between separate {@code tell(...)} and
     * {@code ask(...)} calls.</p>
     *
     * <p>In the Firmata implementation, this configuration is board-wide and
     * the largest delay requested by any device is retained. Requesting a
     * smaller value does not reduce the current delay.</p>
     *
     * @param delay firmware register-to-read delay in microseconds (0..16383)
     * @throws IllegalArgumentException if the Firmata delay is out of range
     * @throws IOException when delay cannot be set due to communication fail
     */
    void setDelay(int delay) throws IOException;

    /**
     * Enables a repeated start between register selection and reading, instead
     * of a STOP. Requires protocol 2.5.1 firmware and a registered read. The
     * default is false; it does not change separate {@link #tell(byte...)} writes.
     * Changes apply to subsequent requests, not already running continuous reads.
     *
     * <p>Enable this when a sensor datasheet requires register selection and
     * reading without a STOP between them. For example:</p>
     * <pre>{@code
     * sensor.setReadRestart(true);
     * sensor.ask(0xD0, (byte) 1, event -> {
     *     int chipId = event.getData()[0] & 0xFF;
     * });
     * }</pre>
     * <p>Leave disabled when the sensor requires a STOP or when using a
     * command/response protocol such as SGP30's separate command write and
     * delayed register-less read. Call {@code setReadRestart(false)} to restore
     * STOP for future requests. Protocol version reports omit the patch version,
     * so callers must know their firmware supports the 2.5.1 flag.</p>
     *
     * @param enabled whether registered reads should use repeated start
     * @throws UnsupportedOperationException if enabling this on an implementation
     * that does not support repeated start
     */
    default void setReadRestart(boolean enabled) {
        if (enabled) {
            throw new UnsupportedOperationException("Repeated-start reads are not supported by this transport.");
        }
    }

    /**
     * Sends data to the I2C device.
     * 
     * @param data data to send
     * @throws IOException when the data cannot be sent due to communication
     * fail
     */
    void tell(byte... data) throws IOException;

    /**
     * Requests data from I2C device and receives it only once. Specified
     * listener is exclusive processor of the received data.
     * 
     * @param responseLength length of expected response
     * @param listener processor of the response
     * @throws IOException when I2C device cannot be asked for data due to 
     * communication fail
     */
    void ask(byte responseLength, I2CListener listener) throws IOException;

    /**
     * Requests data from I2C device and receives it only once. Specified
     * listener is exclusive processor of the received data.
     * 
     * @param register Device register to read from.
     * @param responseLength length of expected response
     * @param listener processor of the response
     * @throws IOException when I2C device cannot be asked for data due to 
     * communication fail
     */
    void ask(int register,byte responseLength, I2CListener listener) throws IOException;


    /**
     * Registers a listener as a receiver of regular updates from I2C device.
     * <br/>
     * The listener starts receiving updates after
     * {@link #startReceivingUpdates} invocation.
     * 
     * @param listener the object that receives updates
     */
    void subscribe(I2CListener listener);

    /**
     * Unregisters a listener from receiving of regular updates from I2C device.
     * 
     * @param listener the object that used to receive updates
     */
    void unsubscribe(I2CListener listener);

    /**
     * Tells I2C device to send data continuously from specified register as it
     * available.
     * 
     * @param register the device register to read from
     * @param messageLength length of expected message
     * @throws IOException when I2C device cannot be asked for data due to 
     * communication fail
     * @return true if receiving updates just started or false otherwise (if it
     * have been already going for example)
     */
    boolean startReceivingUpdates(int register, byte messageLength) throws IOException;
    
    /**
     * Tells I2C device to send data continuously as it available.
     * 
     * @param messageLength length of expected message
     * @throws IOException when I2C device cannot be asked for data due to 
     * communication fail
     * @return true if receiving updates just started or false otherwise (if it
     * have been already going for example)
     */
    boolean startReceivingUpdates(byte messageLength) throws IOException;

    /**
     * Tells I2C device to stop sending data continuously.
     * 
     * @throws IOException when I2C device cannot be asked to stop due to 
     * communication fail
     */
    void stopReceivingUpdates() throws IOException;

}
