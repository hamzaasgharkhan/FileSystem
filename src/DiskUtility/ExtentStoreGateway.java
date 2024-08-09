package DiskUtility;

import Constants.DATA_STORE_BLOCK_FRAME;
import Constants.EXTENT_STORE_FRAME;
import Constants.VALUES;
import Utilities.BinaryUtilities;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

class ExtentStoreGateway {
    private final BitMapUtility bitMapUtility;
    private final SecretKey key;
    private final File extentStoreFile;

    ExtentStoreGateway(File baseFile, BitMapUtility bitMapUtility, SecretKey key) throws Exception{
        File file;
        try {
            file = Gateway.getFileInBaseDirectory(baseFile, Store.ExtentStore.fileName);
        } catch (Exception e){
            throw new Exception("Unable to Initialize ExtentStore: ExtentStore File Inaccessible -- " + e.getMessage());
        }
        extentStoreFile = file;
        this.bitMapUtility = bitMapUtility;
        this.key = key;
    }
    static class ExtentFrame {
        long dataStoreIndex;
        int offset;
        long length;
        long nextAddress;
        ExtentFrame(long dataStoreIndex, int offset, long length) {
            this.dataStoreIndex = dataStoreIndex;
            this.offset = offset;
            this.length = length;
        }
        void setNextAddress(long nextAddress){this.nextAddress = nextAddress;}

        /**
         * This method takes an extentFrame and calculates the index of the block after it along with the byte index of
         * its first byte.
         * @param extentFrame The target ExtentFrame object
         * @return An array of two long integers. The first value is the index of the next block. The second value is
         * the first byte index of the block.
         */
        static long[] getNextBlock(ExtentFrame extentFrame){
            long additionalBlocks = (extentFrame.length + extentFrame.offset) / DATA_STORE_BLOCK_FRAME.DATA_SIZE;
            long lastByteIndex = (extentFrame.length + extentFrame.offset) % DATA_STORE_BLOCK_FRAME.DATA_SIZE;
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
                long[] runningDetails = getNextBlock(runningExtent);
                // Check the following two cases:
                // 1. Both extents are in the same block and are contiguous
                // 2. The extents are in consecutive blocks and contiguous
                // In both cases, extend the length of the first extent to incorporate the second extent.
                if ((currentExtent.dataStoreIndex == runningDetails[0] &&
                    currentExtent.offset == runningDetails[1])){
                    runningExtent.length += currentExtent.length;
                } else {
                    outputExtentFrames.add(runningExtent);
                    runningExtent = currentExtent;
                    if (i == inputExtentFrames.size() - 1){
                        outputExtentFrames.add(runningExtent);
                        return outputExtentFrames;
                    }
                }
            }
            outputExtentFrames.add(runningExtent);
            return outputExtentFrames;
        }
    }
    static byte[] getDefaultBytes(){
        return new byte[EXTENT_STORE_FRAME.SIZE * 16];
    }

    /**
     * This method takes a LinkedList of ExtentFrame objects and creates appropriate ExtentFrame entries for the frames.
     * @param extentFrames LinkedList of target ExtentFrame objects
     * @return An array of two longs. The first element is the extentAddress of the entry and the second element is the
     * length of the extent entries (length of run).
     * @throws Exception in case of IOErrors handling the extentStore file.
     */
    public long[] addExtentEntry(LinkedList<ExtentFrame> extentFrames) throws Exception{
        // To access the extentStore file.
        RandomAccessFile file;
        // Create a new byte array to hold all the entries of the extentFrames LinkedList.
        byte[] byteArray = new byte[EXTENT_STORE_FRAME.FULL_SIZE];
        // Fill the byte array with the extent entries.
        try{
            file = new RandomAccessFile(extentStoreFile, "rw");
        } catch (FileNotFoundException e){
            throw new Exception("Error opening extentStore file. File not found." + e.getMessage());
        }
        long firstIndex = 0, index = 0, nextIndex = 0;
        int extentFramesSize = extentFrames.size();
        if (extentFramesSize == 1){
            firstIndex = index = nextIndex = bitMapUtility.getFreeIndexExtentStore();
            __addExtentEntry(byteArray, extentFrames.getFirst(), nextIndex);
            __writeExtentFrameToFile(file, byteArray, index);
            bitMapUtility.setIndexExtentStore(index, true);
        } else {
            for (int i = 0; i < extentFramesSize; i++){
                if (i == 0) {
                    firstIndex = index = bitMapUtility.getFreeIndexExtentStore();
                    bitMapUtility.setIndexExtentStore(index, true);
                    nextIndex = bitMapUtility.getFreeIndexExtentStore();
                } else if (i + 1 < extentFramesSize){
                    index = nextIndex;
                    bitMapUtility.setIndexExtentStore(index, true);
                    nextIndex = bitMapUtility.getFreeIndexExtentStore();
                } else {
                    index = nextIndex;
                    bitMapUtility.setIndexExtentStore(index, true);
                }
                __addExtentEntry(byteArray, extentFrames.get(i), nextIndex);
                __writeExtentFrameToFile(file, byteArray, index);
            }
        }
        try {
            file.close();
        } catch (IOException e){
            throw new Exception("IOError occurred while closing extentStore file." + e.getMessage());
        }
        return new long[]{firstIndex, extentFrames.size()};
    }

    /**
     * This method takes a byteArray and an extentFrame. It fills the byteArray with the contents of the extentFrame
     * @param byteArray The byteArray containing the extents.
     * @param extentFrame The target extentFrame
     * @param nextAddress Address of the next extent in the run
     */
    private void __addExtentEntry(byte[] byteArray, ExtentFrame extentFrame, long nextAddress) throws Exception{
        byte[] extentBytes = new byte[EXTENT_STORE_FRAME.SIZE];
        System.arraycopy(
                VALUES.MAGIC_VALUE_BYTES,
                0,
                extentBytes,
                EXTENT_STORE_FRAME.MAGIC_VALUE_INDEX,
                4);
        System.arraycopy(
                BinaryUtilities.convertLongToBytes(extentFrame.dataStoreIndex),
                0,
                extentBytes,
                EXTENT_STORE_FRAME.DATA_STORE_INDEX_INDEX,
                8);
        System.arraycopy(
                BinaryUtilities.convertIntToBytes(extentFrame.offset),
                0,
                extentBytes,
                EXTENT_STORE_FRAME.DATA_STORE_OFFSET_INDEX,
                4);
        System.arraycopy(
                BinaryUtilities.convertLongToBytes(extentFrame.length),
                0,
                extentBytes,
                EXTENT_STORE_FRAME.LENGTH_INDEX,
                8);
        System.arraycopy(
                BinaryUtilities.convertLongToBytes(nextAddress),
                0,
                extentBytes,
                EXTENT_STORE_FRAME.NEXT_EXTENT_ADDRESS_INDEX,
                8);
        System.arraycopy(
                Crypto.encryptBlock(extentBytes, key, EXTENT_STORE_FRAME.SIZE),
                0,
                byteArray,
                0,
                EXTENT_STORE_FRAME.FULL_SIZE
        );
    }

    /**
     * This method takes an extentStoreAddress and an extentCount. It returns a LinkedList containing all the ExtentFrames
     * at the provided address.
     * @param extentStoreAddress Target ExtentStore Address
     * @param extentCount Number of ExtentEntries
     * @return A LinkedList containing all the ExtentFrames
     */
    public LinkedList<ExtentFrame> getExtentFrames(long extentStoreAddress, long extentCount) throws Exception{
        LinkedList<ExtentFrame> extentFrames = new LinkedList<ExtentFrame>();
        byte[] byteArray = new byte[EXTENT_STORE_FRAME.FULL_SIZE];
        RandomAccessFile file;
        long index = 0;
        try{
            file = new RandomAccessFile(extentStoreFile, "r");
        } catch (FileNotFoundException e){
            throw new Exception("Error opening extentStore file. File not found." + e.getMessage());
        }
        for (int i = 0; i < extentCount; i++){
            if (i == 0)
                index = extentStoreAddress;
            try{
                file.seek(index * EXTENT_STORE_FRAME.FULL_SIZE);
                file.readFully(byteArray);
            } catch (IOException e){
                throw new Exception("IOError occurred while accessing extentStore file." + e.getMessage());
            }
            ExtentFrame extentFrame = __getExtentEntry(byteArray);
            long nextAddress = extentFrame.nextAddress;
            if (i != extentCount - 1 && index == nextAddress){
                throw new Exception("Mismatch between INodeStore and ExtentStore. Size of Runs Not Identical.");
            }
            index = nextAddress;
            extentFrames.add(extentFrame);
        }
        try {
            file.close();
        } catch (IOException e){
            throw new Exception("IOError occurred while closing extentStore file." + e.getMessage());
        }
        return extentFrames;
    }

    /**
     * This method takes a byteArray containing the encrypted contents of an ExtentFrame and returns the associated
     * ExtentFrame
     * @param byteArray ByteArray containing the Encrypted Form of an ExtentFrame
     * @return The Target Extent Frame
     */
    private ExtentFrame __getExtentEntry(byte[] byteArray) throws Exception{
        byteArray = Crypto.decryptBlock(byteArray, key, EXTENT_STORE_FRAME.SIZE);
        long dataStoreIndex, length, nextAddress;
        int offset;
        dataStoreIndex = BinaryUtilities.convertBytesToLong(byteArray, EXTENT_STORE_FRAME.DATA_STORE_INDEX_INDEX);
        length = BinaryUtilities.convertBytesToLong(byteArray, EXTENT_STORE_FRAME.LENGTH_INDEX);
        offset = BinaryUtilities.convertBytesToInt(byteArray, EXTENT_STORE_FRAME.DATA_STORE_OFFSET_INDEX);
        nextAddress = BinaryUtilities.convertBytesToLong(byteArray, EXTENT_STORE_FRAME.NEXT_EXTENT_ADDRESS_INDEX);
        ExtentFrame extentFrame = new ExtentFrame(dataStoreIndex, offset, length);
        extentFrame.setNextAddress(nextAddress);
        return extentFrame;
    }

    private void __writeExtentFrameToFile(RandomAccessFile file, byte[] byteArray, long index) throws Exception{
        try{
            file.seek(index * EXTENT_STORE_FRAME.FULL_SIZE);
            file.write(byteArray);
        } catch (IOException e){
            throw new Exception("IOError occurred while accessing extentStore file." + e.getMessage());
        }
    }

    /**
     * This method takes the ExtentStore Address and the ExtentCount to delete the target Extent Entry from the Extent
     * Store
     * @param extentFrames LinkedList of the ExtentFrames that need to be removed.
     */
    public void removeExtentEntry(LinkedList<ExtentFrame> extentFrames) throws Exception{
        // Only need to change the bitmap utility to show the occupied locations as empty and that's it.
        for (ExtentFrame extentFrame : extentFrames) {
            try {
                bitMapUtility.setIndexExtentStore(extentFrame.nextAddress, false);
            } catch (Exception e){
                throw new Exception("Unable to remove Extent Entry: " + e.getMessage());
            }
        }
    }
}
