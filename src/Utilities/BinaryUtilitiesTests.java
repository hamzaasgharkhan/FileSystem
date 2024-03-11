package Utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BinaryUtilitiesTests {
    byte[] arr = {
            0x23, 0x12, 0x11, 0x14, 0x41, 0x77,
            0x12, 0x46, 0x69, 0x6c, 0x65, 0x53,
            0x79, 0x73, 0x74, 0x65, 0x6D, 0x0a,
            0x12, 0x78, 0x11, 0x12, 0x6D, 0x24
    };
    @Test
    @DisplayName("convertBytesToInt with a valid starting range")
    public void convertBytesToInt(){
        int result = BinaryUtilities.convertBytesToInt(arr, 2);
        Assertions.assertEquals(286540151 , result);
    }

    @Test
    @DisplayName("convertIntToBytes")
    public void convertIntToBytes(){
        byte[] expected = {0x32, 0x12, 0x11, 0x41};
        Assertions.assertArrayEquals(expected, BinaryUtilities.convertIntToBytes(840044865));
    }

    @Test
    @DisplayName("convertShortToBytes")
    public void convertShortToBytes(){
        byte[] expected = {0x11, 0x14};
        short integer = 0x1114;
        Assertions.assertArrayEquals(expected, BinaryUtilities.convertShortToBytes(integer));
    }

    @Test
    @DisplayName("convertBytesToShort with a valid starting range")
    public void convertBytesToShort(){
        int result = BinaryUtilities.convertBytesToShort(arr, 2);
        Assertions.assertEquals(0x1114 , result);
    }

    @Test
    @DisplayName("convertLongToBytes")
    public void convertLongToBytes(){
        byte[] expected = {0x00, 0x20, (byte)0xC4, (byte)0x93, 0x74, (byte)0xDF, 0x67, (byte)0xFF};
        System.out.println(Long.MAX_VALUE);
        Assertions.assertArrayEquals(expected, BinaryUtilities.convertLongToBytes(9223336854775807L));
    }

    @Test
    @DisplayName("convertBytesToLong with a valid starting range")
    public void convertBytesToLong(){
        long result = BinaryUtilities.convertBytesToLong(arr, 6);
        Assertions.assertEquals(1316855855329802611L , result);
    }

    @Test
    @DisplayName("convertBytesToString Trimmed Whitespaces to prevent the whitespace problem.")
    public void convertBytesToString(){
        String actual = BinaryUtilities.convertBytesToASCIIString(arr, 7, 10);
        String expected = "FileSystem";
        Assertions.assertEquals(expected.trim(), actual.trim());
    }

    @Test
    @DisplayName("getFirstFreeIndex Possible Index")
    public void getFirstFreeIndex(){
        int actual = BinaryUtilities.getFirstFreeIndex((byte)0b01110000, 2);
        int expected = 4;
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("getFirstFreeIndex No Index Possible")
    public void getFirstFreeIndexNoPossibleIndex(){
        int actual = BinaryUtilities.getFirstFreeIndex((byte)0b00001111, 4);
        int expected = -1;
        Assertions.assertEquals(expected, actual);
    }
    @Test
    @DisplayName("getFirstUsedIndex Possible Index")
    public void getFirstUsedIndex(){
        int actual = BinaryUtilities.getFirstUsedIndex((byte)0b00001000, 2);
        int expected = 4;
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("getFirstUsedIndex No Index Possible")
    public void getFirstUsedIndexNoPossibleIndex(){
        int actual = BinaryUtilities.getFirstUsedIndex((byte)0b11110000, 4);
        int expected = -1;
        Assertions.assertEquals(expected, actual);
    }


}
