package org.firmata4j.SGP30;

// SGP30 Token interface JS25

/* SGP30 constants for I2C communication.
 *
 * Based on approach taken by Oleg Kurbatov and the SSD1306Token interface file.
 */
public interface SGP30Token {
    final byte SGP30_ADDR = (byte) 0x58;
    final int[] SGP30FeatureSets = new int[]{0x0020, 0x0022};
//    final byte BMP280_ADDR = (byte) 0x77;
//    final byte BMP280_ID_ASK = (byte) 0xD0;
//    final byte BMP280_ID_ASK = (byte) 0xD0;
//    final byte BMP280_RESET = (byte) 0xE0;
//    final byte BMP280_STATUS = (byte) 0xF3;
//    final byte BMP280_CTRL_MEAS = (byte) 0xF4;
//    final byte BMP280_CONFIG = (byte) 0xF5;

    /* the chip should respond with this ID value when asked */
//    final byte BMP280_ID_DESIRED_RESPONSE = (byte) 0x58;
}