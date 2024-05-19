package DiskUtility;
import Constants.DATA_STORE_BLOCK_FRAME;
import Utilities.BinaryUtilities;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * This class provides an interface between the DataStore files and the rest of the filesystem.
 */
class DataStoreGateway {
    protected static class DataBlock {
        static String getHash(byte[] arr){
            if (arr.length != 4096)
                throw new RuntimeException("Invalid Data block. Array should have 4096 bytes.");
            return BinaryUtilities.convertBytesToASCIIString(arr, DATA_STORE_BLOCK_FRAME.MD5_HASH_INDEX, 16);
        }
        static void setHash(byte[] arr, String hash){
            if (arr.length != 4096)
                throw new RuntimeException("Invalid Data block. Array should have 4096 bytes.");
            if (hash.length() != 128)
                throw new RuntimeException("Invalid Hash. Hash must be 128 bytes.");
            System.arraycopy(hash.getBytes(StandardCharsets.US_ASCII), 0, arr, DATA_STORE_BLOCK_FRAME.MD5_HASH_INDEX, 16);
        }
        static byte getFlags(byte[] arr){
            if (arr.length != 4096)
                throw new RuntimeException("Invalid Data block. Array should have 4096 bytes.");
            return arr[DATA_STORE_BLOCK_FRAME.FLAGS_INDEX];
        }
        static short getBytesOccupied(byte[] arr){
            if (arr.length != 4096)
                throw new RuntimeException("Invalid Data block. Array should have 4096 bytes.");
            return BinaryUtilities.convertBytesToShort(arr, DATA_STORE_BLOCK_FRAME.BYTES_OCCUPIED_INDEX);
        }

        static void setBytesOccupied(byte[] arr, short value){
            if (arr.length != 4096)
                throw new RuntimeException("Invalid Data block. Array should have 4096 bytes.");
            System.arraycopy(BinaryUtilities.convertShortToBytes(value), 0, arr, DATA_STORE_BLOCK_FRAME.BYTES_OCCUPIED_INDEX, 2);
        }

        /**
         * Returns consecutive bytes that are empty.
         * @param arr The Block Array
         * @return an array whose number of total elements is always a multiple of 2. The first element is the start of the
         * run. The following element is the length of the run. This pair repeats itself.
         */
        static int[] getRuns(byte[] arr){
            if (arr.length != 4096)
                throw new RuntimeException("Invalid Data block. Array should have 4096 bytes.");
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
                                    end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + 7;
                                    runs.add(start);
                                    runs.add(end + 1 - start); // To convert indices into length, add 1 to account for the 0 index.
                                    return runs.stream().mapToInt(x->x).toArray();
                                }
                                break;
                            }
                            // If end has been found, add the run to the linked list.
                            end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + end;
                            runs.add(start);
                            runs.add(end + 1 - start); // To convert indices into length, add 1 to account for the 0 index.
                            start = -1;
                        }
                    }
                } else { // -> Case 2: Start has been set.
                    // In case the entire block is free, move to the next byte.
                    if (arr[i] == 0) {
                        // If it is the last byte, set the last index as the end of run and return.
                        if (i == DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX - 1){
                            end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + 7;
                            runs.add(start);
                            runs.add(end + 1 - start); // To convert indices into length, add 1 to account for the 0 index.
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
                            end = (i - DATA_STORE_BLOCK_FRAME.BITMAP_INDEX) * 8 + end;
                            j = end;
                            runs.add(start);
                            runs.add(end + 1 - start); // To convert indices into length, add 1 to account for the 0 index.
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
            if (arr.length != 4096)
                throw new RuntimeException("Invalid Data block. Array should have 4096 bytes.");
            int bitValue = (value) ? 1: 0;
            // Set the bitmaps.
            int startByteIndex = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + (start / 8);
            int startBitIndex = (start % 8);    // Subtracted from 7 to ensure that the leftmost bit is index 0
            int endByteIndex = DATA_STORE_BLOCK_FRAME.BITMAP_INDEX + (start + (length / 8));
            int endBitIndex = (length % 8);     // Subtracted from 7 to ensure that the leftmost bit is index 0
            // Case 1: Both the start and end indices lie in the same byte
            if (startByteIndex == endByteIndex){
                arr[startByteIndex] = BinaryUtilities.setByteIndices(arr[startByteIndex], startBitIndex, endBitIndex, bitValue);
                return;
            }
            // Case 2: The start and end indices lie in different bytes
            arr[startByteIndex] = BinaryUtilities.setByteIndices(arr[startByteIndex], startBitIndex, 7, bitValue);
            for (int i = startByteIndex + 1; i < endByteIndex; i++){
                if (bitValue == 1)
                    arr[i] = -1;
                else
                    arr[i] = 0;
            }
            if (endBitIndex != 0 && endByteIndex < DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX)
                arr[endByteIndex] = BinaryUtilities.setByteIndices(arr[endByteIndex], 0, endBitIndex - 1, bitValue);
        }
    }

    File dataStoreFile;
    final BitMapUtility bitMapUtility;
    DataStoreGateway(Path path, BitMapUtility bitMapUtility) throws Exception {
        File file = path.resolve("data-store").toFile();
        if (!file.exists())
            throw new Exception("Directory Store File Does Not Exist.");
        this.bitMapUtility = bitMapUtility;
        dataStoreFile = file;
    }
    LinkedList<ExtentStoreGateway.ExtentFrame> addNode(File file) throws Exception{
        // A LinkedList of ExtentFrame that will be used in the end to create extents that span across multiple blocks.
        LinkedList<ExtentStoreGateway.ExtentFrame> extentFramesLinkedList = new LinkedList<ExtentStoreGateway.ExtentFrame>();
        long fileSize, bytesToWrite, index;
        // The bytesOccupied field of the block
        short bytesOccupied;
        // data-store file.
        RandomAccessFile dataFile;
        // user input file.
        RandomAccessFile inputFile;
        // pointer within the inputBlock
        int pointer = 0;
        // Contains the data of the user input file
        byte[] inputBlock = new byte[4096];
        // Contains a data block within data-store
        byte[] dataBlock;
        bytesToWrite = fileSize = file.length();
        if (fileSize == 0L)
            throw new Exception("Cannot Call addFile on a directory.");
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // FIX THE TRY CATCH. NOT EVERYTHING SHOULD BE IN THE TRY CATCH.
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        try {
            inputFile = new RandomAccessFile(file, "r");
            dataFile = new RandomAccessFile(dataStoreFile, "rw");
            if (fileSize > 4096){
                inputFile.readFully(inputBlock, 0, 4096);
            } else {
                inputFile.readFully(inputBlock, 0, (int)fileSize);
            }
        } catch (FileNotFoundException e){
            throw new Exception("File being added by user not found or data-store file not found." + e.getMessage());
        } catch (EOFException e){
            throw new Exception("Reading more bytes than the file contains." + e.getMessage());
        } catch (IOException e){
            throw new Exception("Unable to read data-store file or user input file." + e.getMessage());
        } catch (Exception e){
            throw new Exception("Unknown error." + e.getMessage());
        }
        // GET RUNS. WRITE TO THOSE RUNS. KEEP WRITING AND GETTING NEW INDEXES TILL ENTIRE WRITE IS DONE.
        // CREATE APPROPRIATE EXTENT ENTRIES
        // CREATE AN APPROPRIATE INODE
        // RETURN THE INODE
        while (bytesToWrite > 0){
            int bytesWrittenInRunningBlock = 0;
            dataBlock = new byte[4096];
            index = bitMapUtility.getFreeIndexDataStore(bytesToWrite);
            // If the dataBlock is allocated and exists on disk then read it from disk otherwise work with the empty
            // block.
            if (bitMapUtility.isIndexOccupiedDataStore(index)){
                try {
                    dataFile.seek(index * 4096);
                    dataFile.readFully(dataBlock);
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
            int start, end, length;
            for (int i = 0; i < runs.length; i+=2) {
                start = runs[i];
                length = runs[i + 1];
                if ((((pointer + bytesToWrite) < 4096) ||
                        (((pointer + bytesToWrite) > 4096) && ((pointer + length) < 4096)))) {
                    // Case 1: All the data is within the buffer, no more data needs to be read from the file.
                    // If the run can accommodate all the data that is left in the buffer, use the parts of the run
                    // that are necessary and leave the rest of the run empty.
                    // Case 2: All the data of the file is not within the buffer, hence the buffer will need to be
                    // refreshed at some point. However, all the data that the current run can accommodate is in the
                    // buffer, hence we do not need to refresh the buffer for this particular run.
                    // In both cases, the buffer does not need to be updated. The only thing that needs to
                    // be checked is whether we need to write `length` bytes to the dataBlock or `bytesToWrite` bytes
                    // to the dataBlock.
                    // Set the smaller value of the two to localBytesToWrite
                    int localBytesToWrite = (length > bytesToWrite) ? (int) bytesToWrite : length;
                    System.arraycopy(inputBlock, pointer, dataBlock, DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX +  start, localBytesToWrite);
                    // The bytes that are written will be set to 0 within the inputBlock array. Fix this issue.
                    DataBlock.setRunOccupied(dataBlock, start, localBytesToWrite, true);
                    bytesOccupied += (short) localBytesToWrite;
                    bytesWrittenInRunningBlock += localBytesToWrite;
                    pointer += localBytesToWrite;
                    extentFramesLinkedList.add(new ExtentStoreGateway.ExtentFrame(index, start, localBytesToWrite));
                } else if (((pointer + bytesToWrite) > 4096) && (pointer + length) >= 4096) {
                    // In this case, the buffer needs to be refreshed.
                    // The run can accommodate more than what the current buffer has. Buffer needs to be updated.
                    // First copy the data that is in the dataBlock
                    // Set to the smaller value of the two: length of the run and bytesToWrite.
                    int localBytesToWrite = (length > bytesToWrite) ? (int) bytesToWrite : length;
                    int localBytesWritten = 4096 - pointer;
                    System.arraycopy(inputBlock, pointer, dataBlock, DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX +  start, localBytesWritten);
                    // Still more than 4096 bytes left in the file to read
                    try {
                        if (bytesToWrite - localBytesWritten > 4096) {
                            inputFile.readFully(inputBlock, 0, 4096);
                        } else {
                            inputFile.readFully(inputBlock, 0, (int) bytesToWrite - localBytesWritten);
                        }
                    } catch (EOFException e){
                        throw new Exception("Request to read more bytes than the file has left." + e.getMessage());
                    } catch (IOException e){
                        throw new Exception("Unable to access the contents of the user input file." + e.getMessage());
                    }
                    pointer = 0;
                    System.arraycopy(inputBlock, pointer, dataBlock, DATA_STORE_BLOCK_FRAME.FIRST_DATA_BYTE_INDEX + start + localBytesWritten, localBytesToWrite - localBytesWritten);
                    pointer += localBytesToWrite - localBytesWritten;
                    DataBlock.setRunOccupied(dataBlock, start, localBytesToWrite, true);
                    bytesOccupied += (short) localBytesToWrite;
                    bytesWrittenInRunningBlock += localBytesToWrite;
                    extentFramesLinkedList.add(new ExtentStoreGateway.ExtentFrame(index, start, localBytesToWrite));
                } else {
                    throw new Exception("BUG. THIS EXCEPTION SHOULD NEVER BE THROWN.");
                }
                bytesToWrite -= bytesWrittenInRunningBlock;
            }
            DataBlock.setBytesOccupied(dataBlock, bytesOccupied);
            // CREATE FUNCTIONALITY TO ADD THE MD5 HASH for the data block.
            // ALSO CHECK FOR ANY FLAGS IF POSSIBLE
            // DataBlock.setHash()
            bitMapUtility.setIndexDataStore(index, bytesOccupied); // FIX THIS METHOD TO TAKE THE NUMBER OF BYTE THAT ARE OCCUPIED BY THE BLOCK NOW. THE BITMAP SHOULD DECIDE TH EBITMAP
            try {
                dataFile.seek(index * 4096);
                dataFile.write(dataBlock, 0, 4096);
            } catch (IOException e){
                throw new Exception("Error with accessing and writing to the data-store file." + e.getMessage());
            }
        }
        try{
            inputFile.close();
        } catch (IOException e){
            throw new Exception("Unable to close user input file." + e.getMessage());
        }
        try{
            dataFile.close();
        } catch (IOException e){
            throw new Exception("Unable to close data-store file." + e.getMessage());
        }
        return ExtentStoreGateway.ExtentFrame.getCompactExtentList(extentFramesLinkedList);
    }
    static byte[] getDefaultBytes() {
        return new byte[0];
    }
}
