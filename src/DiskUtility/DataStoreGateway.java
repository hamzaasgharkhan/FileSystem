package DiskUtility;
import Constants.DATA_STORE_BLOCK_FRAME;
import Utilities.BinaryUtilities;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * This class provides an interface between the DataStore files and the rest of the filesystem.
 */
class DataStoreGateway {
    protected static class DataBlock {
        static String getHash(byte[] arr){
            if (arr.length != DATA_STORE_BLOCK_FRAME.SIZE)
                throw new RuntimeException("Invalid Data block. Array should have " + DATA_STORE_BLOCK_FRAME.SIZE + " bytes.");
            return BinaryUtilities.convertBytesToASCIIString(arr, DATA_STORE_BLOCK_FRAME.MD5_HASH_INDEX, 16);
        }
        static void setHash(byte[] arr, String hash){
            if (arr.length != DATA_STORE_BLOCK_FRAME.SIZE)
                throw new RuntimeException("Invalid Data block. Array should have " + DATA_STORE_BLOCK_FRAME.SIZE + " bytes.");
            if (hash.length() != 128)
                throw new RuntimeException("Invalid Hash. Hash must be 128 bytes.");
            System.arraycopy(hash.getBytes(StandardCharsets.US_ASCII), 0, arr, DATA_STORE_BLOCK_FRAME.MD5_HASH_INDEX, 16);
        }
        static short getBytesOccupied(byte[] arr){
            if (arr.length != DATA_STORE_BLOCK_FRAME.SIZE)
                throw new RuntimeException("Invalid Data block. Array should have " + DATA_STORE_BLOCK_FRAME.SIZE + " bytes.");
            return BinaryUtilities.convertBytesToShort(arr, DATA_STORE_BLOCK_FRAME.BYTES_OCCUPIED_INDEX);
        }
        static void setBytesOccupied(byte[] arr, short value){
            if (arr.length != DATA_STORE_BLOCK_FRAME.SIZE)
                throw new RuntimeException("Invalid Data block. Array should have " + DATA_STORE_BLOCK_FRAME.SIZE + " bytes.");
            System.arraycopy(BinaryUtilities.convertShortToBytes(value), 0, arr, DATA_STORE_BLOCK_FRAME.BYTES_OCCUPIED_INDEX, 2);
        }

        /**
         * Returns consecutive bytes that are empty.
         * @param arr The Block Array
         * @return an array whose number of total elements is always a multiple of 2. The first element is the start of the
         * run. The following element is the length of the run. This pair repeats itself.
         */
        static int[] getRuns(byte[] arr){
            if (arr.length != DATA_STORE_BLOCK_FRAME.SIZE)
                throw new RuntimeException("Invalid Data block. Array should have " + DATA_STORE_BLOCK_FRAME.SIZE + " bytes.");
            LinkedList<Integer> runs = new LinkedList<Integer>();
            // Control variables for run start and end.
            int start = -1;
            int end;
            for (int i = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX; i < DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX; i++){
                // -> Case 1: Start has not been set yet.
                if (start == -1) {
                    // In case the byte has no free indices, move to the next byte.
                    if (arr[i] == -1)
                        continue;
                    // If the code reaches this point then the byte does contain a possible starting index.
                    end = -1;
                    for (int j = 0; j < 8; j++){
                        // If the start has not been set.
                        if (start == -1){
                            start = j = BinaryUtilities.getFirstFreeIndex(arr[i], j);
                            if (start == -1)
                                break;
                            start = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + start;
                        } else {
                            // If start has been set, look for a possible end in the remaining byte
                            end = BinaryUtilities.getFirstUsedIndex(arr[i], j);
                            // If no end is found within the byte, exit the loop
                            if (end == -1){
                                // If it is the last byte, set the last index as the end and return runs.
                                if (i == DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX - 1){
                                    end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + 8;
                                    runs.add(start);
                                    runs.add(end - start); // To convert indices into length, add 1 to account for the 0 index.
                                    return runs.stream().mapToInt(x->x).toArray();
                                }
                                break;
                            }
                            // If end has been found, add the run to the linked list.
                            j = end;
                            end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + end;
                            runs.add(start);
                            runs.add(end - start); // To convert indices into length, add 1 to account for the 0 index.
                            start = -1;
                        }
                    }
                } else { // -> Case 2: Start has been set.
                    // In case the entire block is free, move to the next byte.
                    if (arr[i] == 0) {
                        // If it is the last byte, set the last index as the end of run and return.
                        if (i == DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX - 1){
                            end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + 8;
                            runs.add(start);
                            runs.add(end - start); // To convert indices into length, add 1 to account for the 0 index.
                            return runs.stream().mapToInt(x->x).toArray();
                        }
                        continue;
                    }
                    // If the code reaches this point then the byte does contain a possible end index.
                    for (int j = 0; j < 8; j++){
                        // If start has been set, look for an end
                        if (start != -1){
                            end = BinaryUtilities.getFirstUsedIndex(arr[i], j);
                            if (end == -1){
                                // If it is the last byte, set the last index as the end and return runs.
                                if (i == 471){
                                    end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + 7;
                                    runs.add(start);
                                    runs.add(end + 1 - start); // To convert indices into length, add 1 to account for the 0 index.
                                    return runs.stream().mapToInt(x->x).toArray();
                                }
                                break;
                            }
                            j = end;
                            end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + end;
                            runs.add(start);
                            runs.add(end - start); // To convert indices into length, add 1 to account for the 0 index.
                            start = -1;
                            end = -1;
                        } else {
                            // If end has been set, look for a possible start in the remaining byte
                            start = BinaryUtilities.getFirstFreeIndex(arr[i], j);
                            // If no start is found within the byte, exit the loop
                            if (start == -1)
                                break;
                            // If start has been found, add the run to the linked list.
                            start = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + start;
                            // Set the counter of the loop to the end index to start from the next index.
                            j = start;
                        }
                    }
                }
            }
            return runs.stream().mapToInt(x->x).toArray();
        }

        /**
         * This method sets the bitmap of the provided indices to 1 (occupied). The indices begin from the start parameter
         * and end at start + length (exclusive)
         * @param arr The block array
         * @param start Starting index of the run
         * @param length length of the run
         * @param value Set to true if the run is occupied. Set to false if the run is not occupied.
         */
        static void setRunOccupied(byte[] arr, int start, int length, boolean value){
            if (arr.length != DATA_STORE_BLOCK_FRAME.SIZE)
                throw new RuntimeException("Invalid Data block. Array should have " + DATA_STORE_BLOCK_FRAME.SIZE + " bytes.");
            int bitValue = (value) ? 1: 0;
            // Set the bitmaps.
            int startByteIndex = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + (start / 8);
            int startBitIndex = (start % 8);    // Subtracted from 7 to ensure that the leftmost bit is index 0
            int endByteIndex = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX +  ((start + length) / 8);
            int endBitIndex = (start + length) % 8; // Non inclusive
            // Case 1: Both the start and end indices lie in the same byte
            if (startByteIndex == endByteIndex){
                arr[startByteIndex] = BinaryUtilities.setByteIndices(arr[startByteIndex], startBitIndex, endBitIndex, bitValue);
                return;
            }
            // Case 2: The start and end indices lie in different bytes
            arr[startByteIndex] = BinaryUtilities.setByteIndices(arr[startByteIndex], startBitIndex, 8, bitValue);
            for (int i = startByteIndex + 1; i < endByteIndex; i++){
                if (bitValue == 1)
                    arr[i] = -1;
                else
                    arr[i] = 0;
            }
            if (endBitIndex != 0)
                arr[endByteIndex] = BinaryUtilities.setByteIndices(arr[endByteIndex], 0, endBitIndex, bitValue);
        }
    }

    private final File dataStoreFile;
    private final BitMapUtility bitMapUtility;
    private final SecretKey key;

    DataStoreGateway(File baseFile, BitMapUtility bitMapUtility, SecretKey key) throws Exception {
        File file;
        try {
            file = Gateway.getFileInBaseDirectory(baseFile, Store.DataStore.fileName);
        } catch (Exception e){
            throw new Exception("Unable to Initialize DataStore: DataStore File Inaccessible -- " + e.getMessage());
        }
        dataStoreFile = file;
        this.bitMapUtility = bitMapUtility;
        this.key = key;
    }

    LinkedList<ExtentStoreGateway.ExtentFrame> addNode(File file) throws Exception{
        // A LinkedList of ExtentFrame that will be used in the end to create extents that span across multiple blocks.
        LinkedList<ExtentStoreGateway.ExtentFrame> extentFramesLinkedList = new LinkedList<ExtentStoreGateway.ExtentFrame>();
        long fileSize, bytesToWrite, index;
        // The bytesOccupied field of the block
        short bytesOccupied;
        // user input file.
        RandomAccessFile inputFile;
        // pointer within the inputBlock
        int pointer = 0;
        // Contains the data of the user input file
        byte[] inputBlock = new byte[DATA_STORE_BLOCK_FRAME.FULL_SIZE];
        // Number of inputBlocks that have been read.
        int inputBlocksRead = 0;
        // Contains a data block within data-store
        byte[] dataBlock = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        bytesToWrite = fileSize = file.length();
        if (fileSize == 0L)
            throw new Exception("Cannot Call addFile on a directory.");
        try {
            inputFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e){
            throw new Exception("inputFile Not Found." + e.getMessage());
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // FIX THE TRY CATCH. NOT EVERYTHING SHOULD BE IN THE TRY CATCH.
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (fileSize >= DATA_STORE_BLOCK_FRAME.FULL_SIZE){
            __fillInputBlock(inputFile, inputBlock, inputBlocksRead, DATA_STORE_BLOCK_FRAME.FULL_SIZE);
        } else {
            __fillInputBlock(inputFile, inputBlock, inputBlocksRead, (int)fileSize);
        }
        inputBlocksRead++;
        while (bytesToWrite > 0){
            int bytesWrittenFromRunningInputBlock = 0;
            Arrays.fill(dataBlock, (byte)0);
            index = bitMapUtility.getFreeIndexDataStore(bytesToWrite);
            if (bitMapUtility.isIndexOccupiedDataStore(index)){
                try {
                        __updateDataBlockArray(dataBlock, index);
                } catch (EOFException e){
                    throw new Exception("Error in dataBlock file. Either the provided index: " + index + " does not exist" +
                            "in the data-store file or the contents of the data-store file have been corrupted." + e.getMessage());
                } catch (IOException e){
                    throw new Exception("Unable to access the contents of data-store file."  + e.getMessage());
                }
            }
            bytesOccupied = DataBlock.getBytesOccupied(dataBlock);
            int[] runs = DataBlock.getRuns(dataBlock);
            if (runs.length == 0)
                throw new Exception("Unexpected Error. Code Has a bug. This Exception should never be printed." +
                        "The index provided by the bitmapUtility does not have a free byte." +
                        "Possible reasons: Bug in Code, Inconsistency between writes and reads");
            if (runs.length % 2 != 0)
                throw new Exception("Unexpected Error. Runs should always have even number of elements. Pairs of" +
                        "start and end indices. Currently runs has an odd number of elements.");
            int start, length;
            for (int i = 0; i < runs.length; i+=2){
                start = runs[i];
                length = runs[i+1];
                // Calculate the number of bytes that will be written to the current RUN.
                int bytesWritableToCurrentRun = (length < bytesToWrite) ? length: (int)bytesToWrite;
                int bytesWrittenToCurrentRun = 0;
                int dataBlockIndex = DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX + start;
                while (bytesWrittenToCurrentRun < bytesWritableToCurrentRun){
                    int localBytesWritten = 0;  // Temporary variable to maintain the number of bytes written
                    int inputBlockLength = inputBlock.length;
                    // CASE 1: inputBlock does not need to be refreshed.
                    if (pointer + bytesWritableToCurrentRun <= inputBlockLength){
                        System.arraycopy(inputBlock, pointer, dataBlock, dataBlockIndex, bytesWritableToCurrentRun);
                        localBytesWritten += bytesWritableToCurrentRun;
                        dataBlockIndex += localBytesWritten;
                        bytesWrittenToCurrentRun += localBytesWritten;
                        bytesToWrite -= localBytesWritten;
                        pointer += localBytesWritten;
                        // ENSURE THAT THE pointer is set to 0 when it reaches inputBlockLength
                        if (pointer == DATA_STORE_BLOCK_FRAME.FULL_SIZE){
                            if (bytesToWrite >= DATA_STORE_BLOCK_FRAME.FULL_SIZE)
                                __fillInputBlock(inputFile, inputBlock, inputBlocksRead, DATA_STORE_BLOCK_FRAME.FULL_SIZE);
                            else
                                __fillInputBlock(inputFile, inputBlock, inputBlocksRead, (int)bytesToWrite);
                            inputBlocksRead++;
                            pointer = 0;
                        }
                    } else {
                        // CASE 2: inputBlock does need to be refreshed
                        System.arraycopy(inputBlock, pointer, dataBlock, dataBlockIndex, inputBlockLength - pointer);
                        localBytesWritten += inputBlockLength - pointer;
                        dataBlockIndex += localBytesWritten;
                        bytesWrittenToCurrentRun += localBytesWritten;
                        bytesToWrite -= localBytesWritten;
                        if (bytesToWrite >= DATA_STORE_BLOCK_FRAME.FULL_SIZE)
                            __fillInputBlock(inputFile, inputBlock, inputBlocksRead, DATA_STORE_BLOCK_FRAME.FULL_SIZE);
                        else
                            __fillInputBlock(inputFile, inputBlock, inputBlocksRead, (int)bytesToWrite);
                        inputBlocksRead++;
                        pointer = 0;
                        if (bytesWritableToCurrentRun - bytesWrittenToCurrentRun > 0){
                            int bytesRemaining = bytesWritableToCurrentRun - bytesWrittenToCurrentRun;
                            System.arraycopy(inputBlock, pointer, dataBlock, dataBlockIndex, bytesRemaining);
                            pointer += bytesRemaining;
                            localBytesWritten += bytesRemaining;
                            dataBlockIndex += bytesRemaining;
                            bytesToWrite -= bytesRemaining;
                            bytesWrittenToCurrentRun += bytesRemaining;
                        }
                    }
                    bytesOccupied += (short) localBytesWritten;
                }
                extentFramesLinkedList.add(new ExtentStoreGateway.ExtentFrame(index, start, bytesWrittenToCurrentRun));
                DataBlock.setRunOccupied(dataBlock, start, bytesWrittenToCurrentRun, true);
                if (bytesToWrite == 0)
                    break;
            }
            DataBlock.setBytesOccupied(dataBlock, bytesOccupied);
//            DataBlock.setHash();
            bitMapUtility.setIndexDataStore(index, bytesOccupied); // FIX THIS METHOD TO TAKE THE NUMBER OF BYTE THAT ARE OCCUPIED BY THE BLOCK NOW. THE BITMAP SHOULD DECIDE TH EBITMAP
            try {
                __updateDataBlockFile(dataBlock, index);
            } catch (IOException e){
                throw new Exception("Error with accessing and writing to the data-store file." + e.getMessage());
            }
        }
        try{
            inputFile.close();
        } catch (IOException e){
            throw new Exception("Unable to close user input file." + e.getMessage());
        }
        return ExtentStoreGateway.ExtentFrame.getCompactExtentList(extentFramesLinkedList);
    }

    private void __fillInputBlock(RandomAccessFile inputFile, byte[] inputBlock, int inputBlocksRead, int bytesToRead) throws Exception{
        try {
            inputFile.seek((long) DATA_STORE_BLOCK_FRAME.FULL_SIZE * inputBlocksRead);
            inputFile.readFully(inputBlock, 0, bytesToRead);
        } catch (FileNotFoundException e){
            throw new Exception("File being added by user not found or data-store file not found." + e.getMessage());
        } catch (EOFException e){
            throw new Exception("Reading more bytes than the file contains." + e.getMessage());
        } catch (IOException e){
            throw new Exception("Unable to read data-store file or user input file." + e.getMessage());
        } catch (Exception e){
            throw new Exception("Unknown error." + e.getMessage());
        }
    }

    public void removeNode(LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames) throws Exception{
        // Set bitmaps of individual blocks within datastore
        // Update byteOccupied of each block
        // Change bitmaps of each block using new bytesOccupied
        // Update the entries in the dataStore.
        byte[] dataBlock = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        for (ExtentStoreGateway.ExtentFrame extentFrame: extentFrames){
            long startBlockIndex, endBlockIndex;
            int startBlockOffset, endBlockOffset;
            startBlockIndex = extentFrame.dataStoreIndex;
            endBlockIndex = extentFrame.dataStoreIndex + ((extentFrame.offset + extentFrame.length) / DATA_STORE_BLOCK_FRAME.DATA_SIZE);
            startBlockOffset = extentFrame.offset;
            endBlockOffset = (int) ((extentFrame.offset + extentFrame.length) % DATA_STORE_BLOCK_FRAME.DATA_SIZE);
            __updateDataBlockArray(dataBlock, startBlockIndex);
            if (startBlockIndex == endBlockIndex){
                // The entire extent is within the same block.
                DataBlock.setRunOccupied(dataBlock, startBlockOffset, (int) extentFrame.length, false);
                short newBytesOccupied = (short)(DataBlock.getBytesOccupied(dataBlock) - extentFrame.length);
                DataBlock.setBytesOccupied(dataBlock, newBytesOccupied);
                __updateDataBlockFile(dataBlock, startBlockIndex);
                bitMapUtility.setIndexDataStore(startBlockIndex, newBytesOccupied);
            } else {
                // The extent spans multiple blocks
                // FIRST: Delete everything in the first block from the offset to the end
                int length = DATA_STORE_BLOCK_FRAME.DATA_SIZE - startBlockOffset;
                DataBlock.setRunOccupied(dataBlock, startBlockOffset, length, false);
                short newBytesOccupied = (short)(DataBlock.getBytesOccupied(dataBlock) - length);
                DataBlock.setBytesOccupied(dataBlock, newBytesOccupied);
                __updateDataBlockFile(dataBlock, startBlockIndex);
                bitMapUtility.setIndexDataStore(startBlockIndex, newBytesOccupied);
                for (long i = startBlockIndex + 1; i < endBlockIndex; i++){
                    // SECOND: Clear all the blocks in between
                    __updateDataBlockArray(dataBlock, i);
                    DataBlock.setRunOccupied(dataBlock, 0, DATA_STORE_BLOCK_FRAME.DATA_SIZE, false);
                    DataBlock.setBytesOccupied(dataBlock, (short)0);
                    __updateDataBlockFile(dataBlock, i);
                    bitMapUtility.setIndexDataStore(i, (short)0);
                }
                // THIRD: Clear the last block till the End Offset
                __updateDataBlockArray(dataBlock, endBlockIndex);
                length = endBlockOffset;
                DataBlock.setRunOccupied(dataBlock, 0, length, false);
                newBytesOccupied = (short)(DataBlock.getBytesOccupied(dataBlock) - length);
                DataBlock.setBytesOccupied(dataBlock, newBytesOccupied);
                __updateDataBlockFile(dataBlock, endBlockIndex);
                bitMapUtility.setIndexDataStore(endBlockIndex, newBytesOccupied);
            }
        }
    }

    /**
     * This method takes a buffer, an extentFrame and an extentIndex. It starts reading within the extent from the given
     * index and tries to fill the buffer. The method returns in two cases: either the buffer is full or all the bytes
     * in the given extent have been read.
     * @param buffer The target buffer to be filled
     * @param extentFrame The target extentFrame to read from
     * @param extentIndex The starting index within the extent
     * @return Number of bytes placed in the buffer
     */
    protected int populateBufferFromExtent(byte[] buffer, ExtentStoreGateway.ExtentFrame extentFrame, int bufferIndex, long extentIndex) throws Exception{
        int bytesWritten = 0;
        byte[] dataBlock = new byte[DATA_STORE_BLOCK_FRAME.SIZE];
        // Fill the Buffer
        long runningBlockIndex = extentFrame.dataStoreIndex + ((extentFrame.offset + extentIndex) / DATA_STORE_BLOCK_FRAME.DATA_SIZE);
        int runningByteIndex = (int) ((extentFrame.offset + extentIndex) % DATA_STORE_BLOCK_FRAME.DATA_SIZE);
        long lastBlockIndex = extentFrame.dataStoreIndex + ((extentFrame.offset + extentFrame.length) / DATA_STORE_BLOCK_FRAME.DATA_SIZE);
        long bytesReadable = extentFrame.length - extentIndex;
        int bytesToWrite= ((buffer.length - bufferIndex) < bytesReadable ? (buffer.length - bufferIndex) : (int) bytesReadable);
        int currentDataBlockBytesRead;
        while (bytesWritten < bytesToWrite){
            try {
                __updateDataBlockArray(dataBlock, runningBlockIndex);
            } catch (Exception e){
                throw new Exception("Unable to populate buffer from extent (DataStore) " + e.getMessage());
            }
            // Set the bytesToRead equal to the number of bytes from the runningByteIndex to the end of the block
            currentDataBlockBytesRead = DATA_STORE_BLOCK_FRAME.DATA_SIZE - runningByteIndex;
            // If the runningBlock is the last block or the bytes available in the DataBlock are greater than can be
            // filled in the buffer, change the size.
            if (runningBlockIndex == lastBlockIndex || currentDataBlockBytesRead > bytesToWrite - bytesWritten) {
                currentDataBlockBytesRead = bytesToWrite - bytesWritten;
            }
            System.arraycopy(dataBlock, DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX + runningByteIndex, buffer, bytesWritten + bufferIndex, currentDataBlockBytesRead);
            bytesWritten += currentDataBlockBytesRead;
            runningByteIndex = 0;
            runningBlockIndex++;
        }
        return bytesWritten;
    }
    /**
     * Takes a byte array representing a datablock and a dataStore Address. Writes the block to the address.
     * @param dataBlock The bytearray containing the target datablock
     * @param address Target DataStore Address
     */
    protected void __updateDataBlockFile(byte[] dataBlock, long address) throws Exception{
        RandomAccessFile fin;
        try {
            fin = new RandomAccessFile(dataStoreFile, "rw");
        } catch (FileNotFoundException e){
            throw new Exception("DataStore FileNotFound: RemoveNode -- DataStoreGateway" + e.getMessage());
        }
        try {
            fin.seek(address * DATA_STORE_BLOCK_FRAME.FULL_SIZE);
        } catch (IOException e){
            throw new Exception("DataStore Unable to seek file. IOException DataStoreGateway" + e.getMessage());
        }
        try {
            dataBlock = Crypto.encryptBlock(dataBlock, key, DATA_STORE_BLOCK_FRAME.SIZE);
            fin.write(dataBlock);
        } catch (IOException e){
            throw new Exception("DataStore Unable to write to file. IOException DataStoreGateway" + e.getMessage());
        } catch (Exception e){
            throw new Exception("DataStore Unable to encrypt dataBlock." + e.getMessage());
        }
        try {
            fin.close();
        } catch (IOException e){
            throw new Exception("Unable to close DataStore File. IOException DataStoreGateway" + e.getMessage());
        }
    }

    /**
     * Takes a byte array and a datastore address. Replaces the content of the bytearray with the new dataBlock.
     * @param dataBlock The target byte array of size `DATA_STORE_BLOCK_FRAME.DATA_STORE_FRAME_SIZE`
     * @param address Target DataStore Address
     */
    protected void __updateDataBlockArray(byte[] dataBlock, long address) throws Exception{
        try{
            System.arraycopy(__getDataBlock(address), 0, dataBlock, 0, DATA_STORE_BLOCK_FRAME.SIZE);
        } catch (Exception e){
            throw new Exception("Unable to decrypt datablock: DataStoreGateway" + e.getMessage());
        }
    }

    /**
     * This method takes an address. It reads the datablock at the given address. Decrypts it and returns the decrypted
     * datablock.
     * @param address Address of the Target Datablock
     * @return byteArray of decrypted datablock.
     * @throws Exception In case of Errors while handling the dataStore File or while decrypting the block.
     */
    protected byte[]  __getDataBlock(long address) throws Exception{
        byte[] byteArray = new byte[DATA_STORE_BLOCK_FRAME.FULL_SIZE];
        RandomAccessFile fin;
        try {
            fin = new RandomAccessFile(dataStoreFile, "r");
        } catch (FileNotFoundException e){
            throw new Exception("DataStore FileNotFound: RemoveNode -- DataStoreGateway" + e.getMessage());
        }
        try {
            fin.seek(address * DATA_STORE_BLOCK_FRAME.FULL_SIZE);
        } catch (IOException e){
            throw new Exception("DataStore Unable to seek file. IOException DataStoreGateway" + e.getMessage());
        }
        try {
            fin.readFully(byteArray);
        } catch (IOException e){
            throw new Exception("DataStore Unable to read from file. IOException DataStoreGateway" + e.getMessage());
        }
        try {
            fin.close();
        } catch (IOException e){
            throw new Exception("Unable to close DataStore File. IOException DataStoreGateway" + e.getMessage());
        }
        try{
            byteArray = Crypto.decryptBlock(byteArray, key, DATA_STORE_BLOCK_FRAME.SIZE);
        } catch (Exception e){
            throw new Exception("Unable to decrypt datablock: DataStoreGateway" + e.getMessage());
        }
        return byteArray;
    }
}