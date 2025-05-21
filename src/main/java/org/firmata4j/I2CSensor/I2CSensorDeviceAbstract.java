package org.firmata4j.I2CSensor;

        import org.firmata4j.firmata.FirmataDevice;
        import org.firmata4j.firmata.FirmataI2CDevice;

public abstract class I2CSensorDeviceAbstract extends FirmataI2CDevice {

    public I2CSensorDeviceAbstract(FirmataDevice device, byte address) {
        super(device, address);
    }

    /**
     * Initialize the sensor (e.g., send startup commands).
     */
    public abstract void initialize();

    /**
     * Trigger a read from the sensor.
     */
    public abstract void readSensor();

    /**
     * Handle the I2C response from the device.
     */
    public abstract void handleResponse(byte[] data);
}
