package DiskUtility;

import java.util.LinkedList;

public class ExtentStoreGateway {
    static class ExtentFrame {
        long dataStoreIndex;
        int offset;
        long length;
        ExtentFrame(long dataStoreIndex, int offset, long length) {
            this.dataStoreIndex = dataStoreIndex;
            this.offset = offset;
            this.length = length;
        }

        /**
         * This method takes an extentFrame and calculates the index of its last block along with the byte index after
         * the end of the extent in the last block.
         * @param extentFrame The target ExtentFrame object
         * @return An array of two long integers. The first value is the index of the last block. The second value is
         * the index of the byte after the extent in the last block.
         */
        static long[] getLastBlock(ExtentFrame extentFrame){
            long additionalBlocks = (extentFrame.length + extentFrame.offset) / 3642;
            long lastByteIndex = (extentFrame.length + extentFrame.offset) % 3642;
            return new long[]{extentFrame.dataStoreIndex + additionalBlocks, lastByteIndex};
        }
        /**
         * This method takes a linked list of extent frames. The extents of the input linked list do not span over
         * multiple blocks. The linked list of extents returned by the method has extents that may span over multiple
         * data blocks.
         *
         * @param inputExtentFrames LinkedList of ExtentFrame objects that are restricted to a single data block.
         * @return A LinkedList of ExtentFrames that may span multiple data blocks.
         */
        static LinkedList<ExtentFrame> getCompactExtentList(LinkedList<ExtentFrame> inputExtentFrames) {
            if (inputExtentFrames.size() < 2){
                return inputExtentFrames;
            }
            LinkedList<ExtentFrame> outputExtentFrames = new LinkedList<ExtentFrame>();
            // This boolean indicates whether an extent has been started. While an extent has been started, others can
            // be appended to it. Once no more are to be appended to it, the boolean will be set to false and that will
            // be the final state of the running extent.
            boolean createNew = true;
            // Sort the extent frames. After sorting, the first extent is guaranteed to be at a lower index. In the
            // case of multiple extents at the same index, they will be sorted based on their offsets within the block.
            inputExtentFrames.sort((ExtentFrame o1, ExtentFrame o2) -> {
                if (o1.dataStoreIndex < o2.dataStoreIndex) {
                    return -1;
                }
                else if (o1.dataStoreIndex > o2.dataStoreIndex) {
                    return 1;
                }
                else {
                    return o1.offset - o2.offset;
                }
            });
            ExtentFrame runningExtent = inputExtentFrames.get(0);
            for (int i = 1; i < inputExtentFrames.size(); i++){
                ExtentFrame currentExtent = inputExtentFrames.get(i);
                long[] runningDetails = getLastBlock(runningExtent);
                if (currentExtent.dataStoreIndex == runningDetails[0] &&
                    currentExtent.offset == runningDetails[1]){
                    runningExtent.length += currentExtent.length;
                } else {
                    outputExtentFrames.add(runningExtent);
                    runningExtent = currentExtent;
                    if (i == inputExtentFrames.size() - 1)
                        outputExtentFrames.add(runningExtent);
                }
            }
            // CHECK IF IT WORKS.
            return outputExtentFrames;
        }
    }

}
