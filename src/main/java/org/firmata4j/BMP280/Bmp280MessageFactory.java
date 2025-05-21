/* 2024-06-11 James Smith's BMP280 Example */

/* Bmp280MessageFactory class */
//Not used here... use later.

package org.firmata4j.BMP280;

/*
  Complex messages for the BMP280 pressure sensor.

  Based on SSD1306MessageFactory by Oleg Kurbatov.

 */
//import static Bmp280Token;
public class Bmp280MessageFactory {
    public static final byte[] BMP280_EXAMPLE_COMMAND_ARRAY_1 = {(byte)0xA0, (byte)0xFF, (byte)0x01}; /* actually means nothing */
    public static final byte[] BMP280_EXAMPLE_COMMAND_ARRAY_2 = {Bmp280Token.BMP280_ID_ASK}; /* actually means nothing */


}