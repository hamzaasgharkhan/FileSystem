package DiskUtility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

public class ExtentStoreGatewayTests {
    @Test
    @DisplayName("getLastBlock Single block extent")
    public void getLastBlockTest1(){
        ExtentStoreGateway.ExtentFrame extentFrame = new ExtentStoreGateway.ExtentFrame(1,90,320);
        long[] output = ExtentStoreGateway.ExtentFrame.getLastBlock(extentFrame);
        long[] expected = {1, 410};
        Assertions.assertArrayEquals(expected, output);
    }
    @Test
    @DisplayName("getLastBlock Multi-block extent")
    public void getLastBlockTest2(){
        ExtentStoreGateway.ExtentFrame extentFrame = new ExtentStoreGateway.ExtentFrame(1,3630,50);
        long[] output = ExtentStoreGateway.ExtentFrame.getLastBlock(extentFrame);
        long[] expected = {2, 38};
        Assertions.assertArrayEquals(expected, output);
    }
    @Test
    @DisplayName("getCompactExtentList Method returns valid extents")
    public void getCompactExtentList(){
        LinkedList<ExtentStoreGateway.ExtentFrame> input = new LinkedList<ExtentStoreGateway.ExtentFrame>();
        input.add(new ExtentStoreGateway.ExtentFrame(1, 32, 18));
        input.add(new ExtentStoreGateway.ExtentFrame(1, 50, 12));
        input.add(new ExtentStoreGateway.ExtentFrame(1, 90, 5));
        input.add(new ExtentStoreGateway.ExtentFrame(1, 120, 7));
        input.add(new ExtentStoreGateway.ExtentFrame(2, 3635, 7));
        input.add(new ExtentStoreGateway.ExtentFrame(3, 0, 3642));
        input.add(new ExtentStoreGateway.ExtentFrame(4, 0, 17));
        input.add(new ExtentStoreGateway.ExtentFrame(4, 72, 18));
        LinkedList<ExtentStoreGateway.ExtentFrame> expected = new LinkedList<ExtentStoreGateway.ExtentFrame>();
        expected.add(new ExtentStoreGateway.ExtentFrame(1, 32, 30));
        expected.add(new ExtentStoreGateway.ExtentFrame(1, 90, 5));
        expected.add(new ExtentStoreGateway.ExtentFrame(1, 120, 7));
        expected.add(new ExtentStoreGateway.ExtentFrame(2, 3635, 3666));
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
