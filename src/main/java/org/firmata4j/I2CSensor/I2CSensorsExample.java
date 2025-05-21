package org.firmata4j.I2CSensor;

        import I2CSensors.AbstractSensorDevice;
        import I2CSensors.SGP30Sensor;
        import I2CSensors.LIS3DHSensor;
        import I2CSensors.DHT20Sensor;

        import org.firmata4j.firmata.FirmataDevice;
        import org.firmata4j.I2CEvent;
        import org.firmata4j.I2CListener;

        import java.util.HashMap;
        import java.util.Map;
        import java.util.concurrent.Executors;
        import java.util.concurrent.ScheduledExecutorService;
        import java.util.concurrent.TimeUnit;

public class I2CSensorsExample {

    public static void main(String[] args) {
        String port = "/dev/ttyUSB0"; // Update this to match your system
        FirmataDevice firmataDevice = new FirmataDevice(port);

        try {
            firmataDevice.start();
            firmataDevice.ensureInitializationIsDone();

            // Create and register sensors
            Map<Byte, I2CSensorDeviceAbstract> sensorMap = new HashMap<>();

            SGP30Sensor sgp30 = new SGP30Sensor(firmataDevice);
            LIS3DHSensor lis3dh = new LIS3DHSensor(firmataDevice);
            DHT20Sensor dht20 = new DHT20Sensor(firmataDevice);

            sensorMap.put(sgp30.getAddress(), sgp30);
            sensorMap.put(lis3dh.getAddress(), lis3dh);
            sensorMap.put(dht20.getAddress(), dht20);

            firmataDevice.addI2CDevice(sgp30);
            firmataDevice.addI2CDevice(lis3dh);
            firmataDevice.addI2CDevice(dht20);

            // Initialize sensors
            sgp30.initialize();
            lis3dh.initialize();
            dht20.initialize();

            // Add I2C listener
            firmataDevice.addI2CListener(new I2CListener() {
                @Override
                public void onReceive(I2CEvent event) {
                    byte address = event.getAddress();
                    byte[] data = event.getData();

                    AbstractSensorDevice sensor = sensorMap.get(address);
                    if (sensor != null) {
                        sensor.handleResponse(data);
                    } else {
                        System.err.println("No sensor registered for address: " + address);
                    }
                }
            });

            // Schedule periodic reads
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                sgp30.readSensor();
                lis3dh.readSensor();
                dht20.readSensor();
            }, 2, 5, TimeUnit.SECONDS); // Initial delay: 2s, repeat every 5s

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
