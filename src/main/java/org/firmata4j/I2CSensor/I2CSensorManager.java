package org.firmata4j.I2CSensor;

        import org.firmata4j.firmata.FirmataDevice;
        import java.util.HashMap;
        import java.util.Map;

public class I2CSensorManager {

    private static I2CSensorManager instance;
    private final FirmataDevice device;
    private final Map<Byte, I2CSensorDeviceAbstract> sensorMap = new HashMap<>();
    private boolean listenerInitialized = false;

    private I2CSensorManager(FirmataDevice device) {
        this.device = device;
    }

    public static synchronized I2CSensorManager getInstance(FirmataDevice device) {
        if (instance == null) {
            instance = new I2CSensorManager(device);
        }
        return instance;
    }

    public void registerSensor(I2CSensorDeviceAbstract sensor) {
        byte address = sensor.getAddress();
        sensorMap.put(address, sensor);
        device.addI2CDevice(sensor);

        if (!listenerInitialized) {
            device.getI2CDataReceivedEvent().addListener(event -> {
                byte addr = event.getAddress();
                AbstractSensorDevice s = sensorMap.get(addr);
                if (s != null) {
                    s.handleResponse(event.getData());
                }
            });
            listenerInitialized = true;
        }
    }
}
