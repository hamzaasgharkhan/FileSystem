package Utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class GeneralUtilitiesTests {
    @Test
    @DisplayName("generateRandomASCIIString returns a string of valid length")
    public void generateRandomASCIIString(){
        String output = GeneralUtilities.generateRandomASCIIString(16);
        System.out.println(output);
        Assertions.assertEquals(16, output.getBytes().length);
    }

    @Test
    @DisplayName("generateRandomASCIIString All bytes are within the acceptable range")
    public void generateRandomASCIIStringValidRange(){
        // The test is done 10,000 times to account for randomness.
        for (int testNumber = 0; testNumber < 10000; testNumber++){
            byte[] output = GeneralUtilities.generateRandomASCIIString(16).getBytes(StandardCharsets.US_ASCII);
            for (int i = 0; i < 16; i++){
                if (!((output[i] > 0x2f && output[i] < 0x3a) ||
                        (output[i] > 0x40 && output[i] < 0x5b) ||
                        (output[i] > 0x60 && output[i] < 0x7b)))
                    Assertions.fail("Invalid ASCII Code found: " + output[i]);
            }
        }
    }

    @Test
    @DisplayName("getFixedSizeUTF8String Check whether output is correct.")
    public void getFixedSizeUTF8StringValidity(){
        String testString = "Osama Bin Laden";
        Assertions.assertEquals(20, GeneralUtilities.getFixedSizeUTF8String(testString, 20).length());
        Assertions.assertEquals(30, GeneralUtilities.getFixedSizeUTF8String(testString, 30).length());
        Assertions.assertEquals(256, GeneralUtilities.getFixedSizeUTF8String(testString, 256).length());
    }

    @Test
    @DisplayName("getFixedSizeUTF8String Check with correct string.")
    public void getFixedSizeUTF8String(){
        String testString = "Osama Bin Laden";
        byte[] expected = {79, 115, 97, 109, 97, 32, 66, 105, 110, 32, 76, 97, 100, 101, 110, 32, 32, 32, 32, 32};
        Assertions.assertArrayEquals(expected, GeneralUtilities.getFixedSizeUTF8StringBytes(testString, 20));
        expected = new byte[]{79, 115, 97, 109, 97, 32, 66, 105, 110, 32, 76, 97, 100, 101, 110, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
                32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32};
        Assertions.assertArrayEquals(expected, GeneralUtilities.getFixedSizeUTF8StringBytes(testString, 256));
    }

    @Test
    @DisplayName("getFixedSizeUTF8String Check with invalid length.")
    public void getFixedSizeUTF8StringInvalidArguments(){
        String testString = "Osama Bin Laden";
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> GeneralUtilities.getFixedSizeUTF8String(testString, 10));
    }

    @Test
    public void randomRum(){
        System.out.println(Arrays.toString(new String("UdnEKs34LKsr1zK2").getBytes(StandardCharsets.US_ASCII)));
        Assertions.assertTrue(true);
    }
}
