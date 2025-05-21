/* 2024-06-11 James Smith's BMP280 Example */

/* Bmp280 class */
/* model this after the SSD1306 class by Oleg Kurbatov */

package org.firmata4j.BMP280;

    import org.firmata4j.I2CDevice;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.io.IOException;
    import java.util.Arrays;

    import static java.lang.Integer.toHexString;

public class Bmp280 {
    private final I2CDevice theBMPdevice;
    private final SensorI2CListener myI2CListener;
    private static final Logger LOGGER = LoggerFactory.getLogger(Bmp280.class);

    /* constructor 1 */
    public Bmp280(I2CDevice theBMPdevice, SensorI2CListener myI2CListener) {
        this.theBMPdevice = theBMPdevice;
        this.myI2CListener = myI2CListener;
    }

    /* Initialize the Bmp280 sensor by asking for its ID. */
    public void init() throws InterruptedException {


        // 1. Tell the chip.
        try{
            theBMPdevice.tell(Bmp280Token.BMP280_ID_ASK);
            System.out.println("I2C telling...");
        }        catch (IOException e){
            throw new RuntimeException(e);
        }

        // 2. Pause briefly.
        Thread.sleep(1);

        // 3. listen.  (ask)
        try {
            theBMPdevice.ask((byte)1, myI2CListener);
            System.out.println("I2C asking...");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread.sleep(20);
        System.out.println("The BMP280 address: (after asking...): 0x" + toHexString(theBMPdevice.getAddress()));

    }


    /* basic I2C command method.
     *  Copied from SSD1306.java */
    private void command (byte... commandBytes){
        try{
            for(int i = 0; i < commandBytes.length; i += 2){
                theBMPdevice.tell(Arrays.copyOfRange(commandBytes,i,i+2));
            }
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}