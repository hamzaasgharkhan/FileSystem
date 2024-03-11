package Utilities;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * This class contains all the basic Utility methods that don't fall into any other class and are not in a large enough
 * quantity to warrant a separate class.
 */
public class GeneralUtilities {
    /**
     * ASCII Characters that are allowed to appear in System File Names
     */
    public static final byte[] acceptableASCIICodes = {
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
            0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
            0x59, 0x5a, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f,
            0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a};

    /**
     * This method returns a random ASCII string within the valid ASCII range as a byte array.
     * @param length The length of the required string (in bytes)
     * @return A random ASCII filename in a byte array.
     */
    public static byte[] generateRandomASCIIStringBytes(int length){
        byte[] randomBytes = new byte[length];
        final int bound = acceptableASCIICodes.length;
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++){
            randomBytes[i] = acceptableASCIICodes[random.nextInt(bound)];
        }
        return randomBytes;
    }

    /**
     * This method returns a random ASCII string within the valid ASCII range.
     * @param length The length of the required string (in bytes)
     * @return A random ASCII filename.
     */
    public static String generateRandomASCIIString(int length){
        return new String(generateRandomASCIIStringBytes(length), StandardCharsets.US_ASCII);
    }

    /**
     * This method takes a String in UTF8 encoding and a length. It returns the input string as a byte array
     * with empty spaces appended to ensure that the length of the output string is equal to the provided length.
     * @param inputString Input String
     * @param length Desired Length (in bytes)
     * @return A byte array containing the string extended to the provided size
     * @throws IllegalArgumentException If length is smaller than the actual length of the string.
     */
    public static byte[] getFixedSizeUTF8StringBytes(String inputString, int length){
        int inputLength = inputString.length();
        if (inputLength > length)
            throw new IllegalArgumentException("inputString is larger than the desired length");
        byte[] stringBytes = new byte[length];
        System.arraycopy(inputString.getBytes(StandardCharsets.UTF_8),
                0,
                stringBytes,
                0,
                inputLength);
        for (int i = inputLength; i < length; i++){
            stringBytes[i] = 0x20;
        }
        return stringBytes;
    }

    /**
     * This method takes a String in UTF8 encoding and a length. It returns the input string with empty spaces appended
     * to ensure that the length of the output string is equal to the provided length
     * @param inputString Input String
     * @param length Desired Length (in bytes)
     * @return String of requested size
     * @throws IllegalArgumentException If length is smaller than the actual length of the string.
     */
    public static String getFixedSizeUTF8String(String inputString, int length){
        return new String(getFixedSizeUTF8StringBytes(inputString, length), StandardCharsets.UTF_8);
    }
}
