package DiskUtility;

import Constants.DATA_STORE_BLOCK_FRAME;
import Constants.EXTENT_STORE_FRAME;
import Constants.VALUES;
import Utilities.BinaryUtilities;
import Utilities.GeneralUtilities;

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
    private File extentStoreFile;
    ExtentStoreGateway(Path path, BitMapUtility bitMapUtility, SecretKey key) throws Exception{
        File file = path.resolve("extent-store").toFile();
        if (!file.exists())
            throw new Exception("Extent Store File Does Not Exist.");
        this.bitMapUtility = bitMapUtility;
        this.key = key;
        extentStoreFile = file;
    }
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
         * This method takes an extentFrame and calculates the index of the block after it along with the byte index of
         * its first byte.
         * @param extentFrame The target ExtentFrame object
         * @return An array of two long integers. The first value is the index of the next block. The second value is
         * the first byte index of the block.
         */
        static long[] getNextBlock(ExtentFrame extentFrame){
            long additionalBlocks = (extentFrame.length + extentFrame.offset) / DATA_STORE_BLOCK_FRAME.DATA_STORE_FRAME_DATA_SIZE;
            long lastByteIndex = (extentFrame.length + extentFrame.offset) % DATA_STORE_BLOCK_FRAME.DATA_STORE_FRAME_DATA_SIZE;
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
        byte[] byteArray = new byte[EXTENT_STORE_FRAME.FULL_SIZE * extentFrames.size()];
        // Fill the byte array with the extent entries.
        for (int i = 0; i < extentFrames.size(); i++){
            __addExtentEntry(byteArray, extentFrames.get(i), i);
        }
        // Get the appropriate index in the extentStore for the extent based on its size.
        long index = bitMapUtility.getFreeIndexExtentStore();
        try{
            file = new RandomAccessFile(extentStoreFile, "rw");
        } catch (FileNotFoundException e){
            throw new Exception("Error opening extentStore file. File not found." + e.getMessage());
        }
        try{
            file.seek(index * EXTENT_STORE_FRAME.SIZE);
            file.write(byteArray);
            bitMapUtility.setIndexExtentStore(index, true);
        } catch (IOException e){
            throw new Exception("IOError occurred while accessing extentStore file." + e.getMessage());
        }
        try {
            file.close();
        } catch (IOException e){
            throw new Exception("IOError occurred while closing extentStore file." + e.getMessage());
        }
        return new long[]{index, extentFrames.size()};
    }

    /**
     * This method takes a byteArray and an extentFrame. It adds the extentFrame to the specified index.
     * @param byteArray The byteArray containing the extents.
     * @param extentFrame The target extentFrame
     * @param index The target index within the byteArray
     */
    private void __addExtentEntry(byte[] byteArray, ExtentFrame extentFrame, int index) throws Exception{
        byte[] extentBytes = new byte[EXTENT_STORE_FRAME.SIZE];
        index = index * EXTENT_STORE_FRAME.SIZE;
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
                Crypto.encryptBlock(extentBytes, key, EXTENT_STORE_FRAME.SIZE),
                0,
                byteArray,
                index,
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
        byte[] byteArray = new byte[(int) (extentCount * EXTENT_STORE_FRAME.FULL_SIZE)];
        RandomAccessFile file;
        try{
            file = new RandomAccessFile(extentStoreFile, "r");
        } catch (FileNotFoundException e){
            throw new Exception("Error opening extentStore file. File not found." + e.getMessage());
        }
        try{
            file.seek(extentStoreAddress * EXTENT_STORE_FRAME.FULL_SIZE);
            file.readFully(byteArray);
        } catch (IOException e){
            throw new Exception("IOError occurred while accessing extentStore file." + e.getMessage());
        }
        try {
            file.close();
        } catch (IOException e){
            throw new Exception("IOError occurred while closing extentStore file." + e.getMessage());
        }
        for (int i = 0; i < extentCount; i++){
            extentFrames.add(__getExtentEntry(byteArray, i));
        }
        return extentFrames;
    }

    /**
     * This method takes a byteArray and an index within the byteArray to return an ExtentFrame at that address
     * @param byteArray Target ByteArray
     * @param index Index within the byteArray
     * @return The Target Extent Frame
     */
    private ExtentFrame __getExtentEntry(byte[] byteArray, int index) throws Exception{
        index = index * EXTENT_STORE_FRAME.FULL_SIZE;
        byte[] extentBytes = Arrays.copyOfRange(byteArray, index, index + EXTENT_STORE_FRAME.FULL_SIZE);
        extentBytes = Crypto.decryptBlock(extentBytes, key, EXTENT_STORE_FRAME.SIZE);
        long dataStoreIndex, length;
        int offset;
        dataStoreIndex = BinaryUtilities.convertBytesToLong(extentBytes, EXTENT_STORE_FRAME.DATA_STORE_INDEX_INDEX);
        length = BinaryUtilities.convertBytesToLong(extentBytes, EXTENT_STORE_FRAME.LENGTH_INDEX);
        offset = BinaryUtilities.convertBytesToInt(extentBytes, EXTENT_STORE_FRAME.DATA_STORE_OFFSET_INDEX);
        return new ExtentFrame(dataStoreIndex, offset, length);
    }

    /**
     * This method takes the ExtentStore Address and the ExtentCount to delete the target Extent Entry from the Extent
     * Store
     * @param extentStoreAddress Address of the target Extent
     * @param extentCount Number of Extent Entries
     */
    public void removeExtentEntry(long extentStoreAddress, long extentCount){
        // Only need to change the bitmap utility to show the occupied locations as empty and that's it.
        for (long i = 0L; i < extentCount; i++){
            bitMapUtility.setIndexExtentStore(extentStoreAddress + i, false);
        }
    }
}
