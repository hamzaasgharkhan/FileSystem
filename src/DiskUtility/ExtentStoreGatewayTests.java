package DiskUtility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

public class ExtentStoreGatewayTests {
    @Test
    @DisplayName("getNextBlock Single block extent")
    public void getNextBlockTest1(){
        ExtentStoreGateway.ExtentFrame extentFrame = new ExtentStoreGateway.ExtentFrame(1,90,320);
        long[] output = ExtentStoreGateway.ExtentFrame.getNextBlock(extentFrame);
        long[] expected = {1, 410};
        Assertions.assertArrayEquals(expected, output);
    }
    @Test
    @DisplayName("getNextBlock Covering entire extent")
    public void getNextBlockTest2(){
        ExtentStoreGateway.ExtentFrame extentFrame = new ExtentStoreGateway.ExtentFrame(1,0,3624);
        long[] output = ExtentStoreGateway.ExtentFrame.getNextBlock(extentFrame);
        long[] expected = {2, 0};
        Assertions.assertArrayEquals(expected, output);
    }
    @Test
    @DisplayName("getNextBlock Multi block extent")
    public void getNextBlockTest3(){
        ExtentStoreGateway.ExtentFrame extentFrame = new ExtentStoreGateway.ExtentFrame(1,0,4000);
        long[] output = ExtentStoreGateway.ExtentFrame.getNextBlock(extentFrame);
        long[] expected = {2, 376};
        Assertions.assertArrayEquals(expected, output);
    }
    @Test
    @DisplayName("getCompactExtentList Method returns valid extents")
    public void getCompactExtentList(){
        LinkedList<ExtentStoreGateway.ExtentFrame> input = new LinkedList<ExtentStoreGateway.ExtentFrame>();
        input.add(new ExtentStoreGateway.ExtentFrame(0, 0, 3624));
        input.add(new ExtentStoreGateway.ExtentFrame(1, 0, 3624));
        input.add(new ExtentStoreGateway.ExtentFrame(2, 0, 3624));
        input.add(new ExtentStoreGateway.ExtentFrame(3, 0, 3624));
        input.add(new ExtentStoreGateway.ExtentFrame(4, 0, 17));
        input.add(new ExtentStoreGateway.ExtentFrame(4, 72, 18));
        LinkedList<ExtentStoreGateway.ExtentFrame> expected = new LinkedList<ExtentStoreGateway.ExtentFrame>();
        expected.add(new ExtentStoreGateway.ExtentFrame(0, 0, 14513));
        expected.add(new ExtentStoreGateway.ExtentFrame(4, 72, 18));
        LinkedList<ExtentStoreGateway.ExtentFrame> output = ExtentStoreGateway.ExtentFrame.getCompactExtentList(input);
        Assertions.assertEquals(expected.size(), output.size());
        for (int i = 0; i < expected.size(); i++){
            ExtentStoreGateway.ExtentFrame expectedFrame = expected.get(i);
            ExtentStoreGateway.ExtentFrame outputFrame = output.get(i);
            Assertions.assertEquals(expectedFrame.dataStoreIndex, outputFrame.dataStoreIndex);
            Assertions.assertEquals(expectedFrame.offset, outputFrame.offset);
            Assertions.assertEquals(expectedFrame.length, outputFrame.length);
        }
    }
}
