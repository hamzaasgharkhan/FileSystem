package DiskUtility;

import Constants.DATA_STORE_BLOCK_FRAME;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DataStoreGatewayTests {

    @Test
    @DisplayName("DATABLOCK getruns()  -> Empty Block")
    public void getRuns1(){
        byte[] block = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        int[] expected = {0, DATA_STORE_BLOCK_FRAME.DATA_SIZE};
        int[] output = DataStoreGateway.DataBlock.getRuns(block);
        Assertions.assertArrayEquals(expected, output);
    }

    @Test
    @DisplayName("DATABLOCK getruns()  -> Pattern: FULL - EMPTY")
    public void getRuns2(){
        byte[] block = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX; i < DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 20; i++){
            block[i] = (byte) -1;
        }
        block[DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 20] = -8;
        int totalBytes = 20 * 8 + 5;
        int[] expected = {totalBytes, DATA_STORE_BLOCK_FRAME.DATA_SIZE - totalBytes};
        int[] output = DataStoreGateway.DataBlock.getRuns(block);
        Assertions.assertArrayEquals(expected, output);
    }

    @Test
    @DisplayName("DATABLOCK getruns()  -> Pattern: EMPTY - FULL")
    public void getRuns3(){
        byte[] block = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 20; i < DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX; i++){
            block[i] = (byte) -1;
        }
        block[DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 19] = 3;
        int totalBytes = (20 * 8) - 2;
        int[] expected = {0, totalBytes};
        int[] output = DataStoreGateway.DataBlock.getRuns(block);
        Assertions.assertArrayEquals(expected, output);
    }

    @Test
    @DisplayName("DATABLOCK getruns()  -> Pattern: EMPTY - FULL - EMPTY - FULL")
    public void getRuns4(){
        byte[] block = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 20; i < DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 80; i++){
            block[i] = (byte) -1;
        }
        block[DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 19] = 3;
        block[DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 80] = -61;
        for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 81; i < DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 120; i++){
            block[i] = (byte) -1;
        }
        for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + 200; i < DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX; i++){
            block[i] = (byte) -1;
        }
        int totalBytes1 = (20 * 8) - 2;
        int totalBytes2 = (80 * 8);
        int[] expected = {0, totalBytes1, 642, 4, 120 * 8, totalBytes2};
        int[] output = DataStoreGateway.DataBlock.getRuns(block);
        Assertions.assertArrayEquals(expected, output);
    }

    @Test
    @DisplayName("DATABLOCK setRunsOccupied() -> Test on empty block")
    public void setRunsOccupied(){
        byte[] block = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        byte[] expected = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX; i < DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX; i++)
            expected[i] = -1;
        DataStoreGateway.DataBlock.setRunOccupied(block, 0, DATA_STORE_BLOCK_FRAME.DATA_SIZE, true);
        Assertions.assertArrayEquals(expected, block);
    }

}
