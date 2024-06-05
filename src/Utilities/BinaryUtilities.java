package Utilities;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This class provides functionalities to deal with binary data.
 */
public class BinaryUtilities {
    /**
     * This method takes an integer and returns the big endian byte array of the integer.
     * @param integer The integer to be converted into a byte array
     * @return The byte array for the input integer in big endian byte ordering
     */
    public static byte[] convertIntToBytes(int integer){
        return ByteBuffer.allocate(4).putInt(integer).array();
    }

    /**
     * This method takes a short integer and returns the big endian byte array of the short.
     * @param integer The short integer to be converted into a byte array
     * @return The byte array for the input integer in big endian byte ordering
     */
    public static byte[] convertShortToBytes(short integer){
        return ByteBuffer.allocate(2).putShort(integer).array();
    }

    /**
     * This method takes a long integer and returns the big endian byte array of the integer.
     * @param longInteger The long integer to be converted into a byte array
     * @return The byte array for the input integer in big endian byte ordering
     */
    public static byte[] convertLongToBytes(long longInteger){
        return ByteBuffer.allocate(8).putLong(longInteger).array();
    }


    /**
     * This method takes an array that must have at least 2 elements. It also takes a starting point and converts the
     * first 2 bytes starting from that point into a short integer. It considers the bytes in a big endian byte ordering.
     * @param arr The array of bytes that contains the short integer
     * @param start The index of the first byte of the short integer
     * @return The required short integer
     * @throws RuntimeException If the array has less than 2 bytes starting from the start index, a RuntimeException is
     * thrown.
     */
    public static short convertBytesToShort(byte[] arr, int start){
        if (arr.length - start < 2){
            throw new RuntimeException("Not enough bytes to convert into a short");
        }
        short result = 0;
        for (int i = 0; i < 2; i++){
            result = (short)((result << 8) | (arr[start+i] & 0xFF));
        }
        return result;
    }

    /**
     * This method takes an array that must have at least 4 elements. It also takes a starting point and converts the
     * first 4 bytes starting from that point into an integer. It considers the bytes in a big endian byte ordering.
     * @param arr The array of bytes that contains the integer
     * @param start The index of the first byte of the integer
     * @return The required integer
     * @throws RuntimeException If the array has less than 4 bytes starting from the start index, a RuntimeException is
     * thrown.
     */
    public static int convertBytesToInt(byte[] arr, int start){
        if (arr.length - start < 4){
            throw new RuntimeException("Not enough bytes to convert into an integer");
        }
        int result = 0;
        for (int i = 0; i < 4; i++){
            result = (result << 8) | (arr[start+i] & 0xFF);
        }
        return result;
    }

    /**
     * This method takes an array that must have at least 8 elements starting from the start index. It also takes a
     * starting point and converts the first 4 bytes starting from that point into an integer. It considers the bytes in
     * a big endian byte ordering.
     * @param arr The array of bytes that contains the integer
     * @param start The index of the first byte of the integer
     * @return The required long
     * @throws RuntimeException If the array has less than 4 bytes starting from the start index, a RuntimeException is
     * thrown.
     */
    public static long convertBytesToLong(byte[] arr, int start){
        if (arr.length - start < 8){
            throw new RuntimeException("Not enough bytes to convert into an integer");
        }
        long result = 0;
        for (int i = 0; i < 8; i++){
            result = (result << 8) | (arr[start+i] & 0xFF);
        }
        return result;
    }

    /**
     * This method takes an array, a starting index and the length of the string and returns an ASCII string from
     * those bytes.
     * @param arr The array of bytes that contains the string
     * @param start index of the first byte
     * @param len the length of the desired string
     * @return The resultant ASCII string.
     */
    public static String convertBytesToASCIIString(byte[] arr, int start, int len){
        byte[] stringArr = Arrays.copyOfRange(arr, start, start + len);
        return new String(stringArr, StandardCharsets.US_ASCII);
    }

    /**
     * This method takes an array, a starting index and the length of the string and returns a UTF-8 string from
     * those bytes.
     * @param arr The array of bytes that contains the string
     * @param start index of the first byte
     * @param len the length of the desired string
     * @return The resultant UTF8 string.
     */
    public static String convertBytesToUTF8String(byte[] arr, int start, int len){
        byte[] stringArr = Arrays.copyOfRange(arr, start, start + len);
        return new String(stringArr, StandardCharsets.UTF_8);
    }
    /**
     * Auxiliary method to return the first free bit in the given byte starting from the given index. This method
     * treats each bit of the byte as an index and the indexing starts at 0 from the most significant bit.
     * @param b The byte to be inspected
     * @param index The beginning index
     * @return The first free index or return -1 if no free index
     */
    public static int getFirstFreeIndex(byte b, int index){
        index = 7 - index;
        for (int i = index; i > -1; i--){
            if(((b >> i) & 1) == 0)
                return 7 - i;
        }
        return -1;
    }
    /**
     * Auxiliary method to return the first used bit in the given byte starting from the given index. This method
     * treats each bit of the byte as an index and the indexing starts at 0 from the most significant bit.
     * @param b The byte to be inspected
     * @param index The beginning index
     * @return The first used index or return -1 if no free index
     */
    public static int getFirstUsedIndex(byte b, int index){
        index = 7 - index;
        for (int i = index; i >= 0; i--){
            if(((b >> i) & 1) == 1)
                return 7 - i;
        }
        return -1;
    }
    /**
     * This method takes a byte, a start index, an end index (exclusive) and a value: 0 or 1. It sets the bits at the
     * given indices to the given binary value.
     * @param b The target byte
     * @param startIndex The index of the first bit
     * @param endIndex The index after the last bit
     * @param bitValue A bit value: 0 or 1
     * @return The modified byte
     */
    public static byte setByteIndices(byte b, int startIndex, int endIndex, int bitValue){
        if (startIndex < 0 || endIndex < startIndex || endIndex > 8)
            throw new IllegalArgumentException("Invalid Indices:Start and End indices must be between 0 and 7 (inclusive)." +
                    " Start should always be less than or equal to the end index");
        if (bitValue != 0 && bitValue != 1)
            throw new IllegalArgumentException("InvalidValue: Value must be either 0 or 1.");
        byte mask = (byte)0b10000000;
        // Fill the mask with as many "1" bits as the length of the run (endIndex - startIndex)
        for (int i = 0; i < (endIndex - startIndex - 1); i++){
            mask >>= 1;
        }
        // Move the first 1 bit to the start position using logical right shift.
        // int is used here to overcome Java's problem with casting to int before shifting.
        int result = ((int) mask & 0xFF) >>> startIndex;
        mask = (byte) result;
        if (bitValue == 1){
            return (byte) (b | mask);
        }
        return (byte) (b & ~mask);
    }
}
