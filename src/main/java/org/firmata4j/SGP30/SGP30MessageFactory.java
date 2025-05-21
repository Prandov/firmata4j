package org.firmata4j.SGP30;
//import static SGP30Token;

// SGP30MessageFactory class
// Not used here... use later. JS25
    /*
      Complex messages for the SGP30 CO2+tVOC sensor.

      Based on SSD1306MessageFactory by Oleg Kurbatov.

     */
public class SGP30MessageFactory {
        public static final byte[] BMP280_EXAMPLE_COMMAND_ARRAY_1 = {(byte)0xA0, (byte)0xFF, (byte)0x01}; /* actually means nothing */
        public static final int[][] BMP280_EXAMPLE_COMMAND_ARRAY_2 = {SGP30Token.SGP30FeatureSets}; /* actually means nothing */
    }
