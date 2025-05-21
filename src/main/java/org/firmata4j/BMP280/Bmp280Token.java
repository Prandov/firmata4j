/* 2024-06-11 James Smith's BMP280 Example */

/* BmpToken interface */

package org.firmata4j.BMP280;



/* BMP280 constants for I2C communicition.
 *
 * Based on approach taken by Oleg Kurbatov and the SSD1306Token interface file.
 */
public interface Bmp280Token {
    final byte BMP280_ADDR = (byte) 0x77;
    final byte BMP280_ID_ASK = (byte) 0xD0;
    final byte BMP280_RESET = (byte) 0xE0;
    final byte BMP280_STATUS = (byte) 0xF3;
    final byte BMP280_CTRL_MEAS = (byte) 0xF4;
    final byte BMP280_CONFIG = (byte) 0xF5;

    /* the chip should respond with this ID value when asked */
    final byte BMP280_ID_DESIRED_RESPONSE = (byte) 0x58;
}
