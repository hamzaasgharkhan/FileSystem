package DiskUtility;

import Constants.DATA_STORE_BLOCK_FRAME;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DataStoreGatewayTests {

    @Test
    @DisplayName("DATABLOCK getruns()  -> Empty Block")
    public void getRuns(){
        byte[] block = new byte[4096];
        int[] expected = {0, DATA_STORE_BLOCK_FRAME.DATA_STORE_FRAME_DATA_SIZE};
        int[] output = DataStoreGateway.DataBlock.getRuns(block);
        Assertions.assertArrayEquals(expected, output);
    }

    @Test
    @DisplayName("DATABLOCK setRunsOccupied() -> Test on empty block")
    public void setRunsOccupied(){
        byte[] block = new byte[4096];
        byte[] expected = new byte[4096];
        for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX; i < DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX; i++)
            expected[i] = -1;
        DataStoreGateway.DataBlock.setRunOccupied(block, 0, DATA_STORE_BLOCK_FRAME.DATA_STORE_FRAME_DATA_SIZE, true);
        Assertions.assertArrayEquals(expected, block);
    }

}
