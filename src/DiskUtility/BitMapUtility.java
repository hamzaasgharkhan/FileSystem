package DiskUtility;

import Constants.DATA_STORE_BLOCK_FRAME;

import javax.management.RuntimeMBeanException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class handles all the interactions between the filesystem and the bitmap files.
 */
public class BitMapUtility {
    private final String parentPath;
    private byte[] directoryStoreBitMap;
    private byte[] extentStoreBitMap;
    private byte[] iNodeStoreBitMap;
    private byte[] dataStoreBitMap;
    private byte[] thumbnailStoreBitMap;
    private byte[] attributeStoreBitMap;
    BitMapUtility(String path) throws Exception{
        parentPath = path;
        __loadBitMaps(path);
    };
    /**
     * This byte denotes whether a particular bitmap is dirty i.e. different from the version on disk.
     * 0x00ATDIED
     * A -> AttributeStore
     * T -> ThumbnailStore
     * D -> DataStore
     * I -> INodeStore
     * E -> ExtentStore
     * D -> DirectoryStore
     */
    private byte dirtyFlags = 0x00;
    /**
     * This method loads all the bitmap files from storage to memory.
     * @param path Path of the fileSystem.
     */
    private void __loadBitMaps(String path) throws Exception{
        if (dirtyFlags != 0)
            throw new RuntimeException("Cannot Load Bitmaps from Storage. Dirty Bitmaps in memory need to be written to storage first");
        byte[][] arrays = new byte[6][];
        String[] paths = {
                "directory-store.bitmap",
                "extent-store.bitmap",
                "inode-store.bitmap",
                "data-store.bitmap",
                "thumbnail-store.bitmap",
                "attribute-store.bitmap"
        };
        for (int i = 0; i < arrays.length; i++){
            try {
                FileInputStream fin = new FileInputStream(path + "/" + paths[0]);
                arrays[i] = fin.readAllBytes();
                fin.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e){
                throw new Exception(e);
            }
        }
        directoryStoreBitMap = arrays[0];
        extentStoreBitMap = arrays[1];
        iNodeStoreBitMap = arrays[2];
        dataStoreBitMap = arrays[3];
        thumbnailStoreBitMap = arrays[4];
        attributeStoreBitMap = arrays[5];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DIRECTORY_STORE METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public long getFreeIndexDirectoryStore(){
        for (int i = 0; i < directoryStoreBitMap.length; i++) {
            if ((byte) directoryStoreBitMap[i] == -1)
                continue;
            else {
                int bitIndex = -1;
                for (int j = 7; j >= 0; j--) {
                    if (((directoryStoreBitMap[i] >> j) & 1) == 0) {
                        bitIndex = 7 - j;
                        break;
                    }
                }
                return i + bitIndex;
            }
        }
        // IF CODE REACHES THIS POINT, THERE IS NO FREE BLOCK in the byte array. need to assign more blocks.
        int ind = directoryStoreBitMap.length;
        byte[] arr = new byte[ind + 16];
        System.arraycopy(directoryStoreBitMap, 0, arr, 0, ind);
        directoryStoreBitMap = arr;
        setDirtyFlag("DIRECTORY_STORE");
        writeToFile("DIRECTORY_STORE");
        return ind * 8L;
    }
    public void toggleIndexDirectoryStore(long index){
        int byteIndex = (int) (index / 8L);
        int bitIndex = (int) (index % 8L);
        byte requiredByte = directoryStoreBitMap[byteIndex];
        requiredByte = (byte)(requiredByte ^ (0x01 << 7-bitIndex));
        directoryStoreBitMap[byteIndex] = requiredByte;
        setDirtyFlag("DIRECTORY_STORE");
        writeToFile("DIRECTORY_STORE");
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DATA_STORE METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public long getFreeIndexDataStore(long bytesToWrite){
        /*
        Basic:
            Return the first block that has free space.
        Optimal:
            Check the fileSize and provide a block index accordingly. Also take into consideration the fact that a file
            can be larger than the current blocks allocated and hence would require the allocation of more blocks.
            Ideally should return the smallest block available to satisfy the fileSize needs.
         */
        for (int i = 0; i < dataStoreBitMap.length; i++){
            byte dataStoreByte = dataStoreBitMap[i];
            if (dataStoreByte == -1) {
                continue;
            }
            if ((dataStoreByte & 0b11110000) != 0b11110000) {
                return (long)i * 2;
            }
            else {
                return (i * 2L) + 1;
            }
        }
        // If the code reaches this point then new blocks need to be allocated.
        int index = dataStoreBitMap.length;
        byte[] arr = new byte[index + 16];
        System.arraycopy(dataStoreBitMap, 0, arr, 0, index);
        byte[] blankSlots = {(byte)0b10011001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001,
                (byte)0b10011001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001,
                (byte)0b10011001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001,
                (byte)0b10011001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001};
        System.arraycopy(blankSlots, 0, arr, index, 16);
        dataStoreBitMap = arr;
        setDirtyFlag("DATA_STORE");
        writeToFile("DATA_STORE");
        return index * 2L;
    }

    /**
     * Update the bitmap at the particular index
     * @param index index of the bitmap needed to be updated
     * @param bytesOccupied Number of bytes occupied by the index
     */
    public void setIndexDataStore(long index, short bytesOccupied){
        // The bitmap is provided in the 4 least significant bits of the byte.
        byte bitmap;
        int totalBytes = DATA_STORE_BLOCK_FRAME.DATA_STORE_FRAME_DATA_SIZE;
        if (bytesOccupied == 0) // block is empty
            bitmap = (byte) 0b00001000;
        else if (bytesOccupied < totalBytes / 4)    // block is less than quarter full
            bitmap = (byte) 0b00000000;
        else if (bytesOccupied < totalBytes / 2)    // block is less than half full but at least quarter full
            bitmap = (byte) 0b00000001;
        else if (bytesOccupied < 3 * (totalBytes / 4)) // block is less than 3/4th full but at least half full
            bitmap = (byte) 0b00000010;
        else if (bytesOccupied < totalBytes)    // block is at least 3/4th full but not completely full
            bitmap = (byte) 0b00000100;
        else    // block is full
            bitmap = (byte) 0b00001000;
        int byteIndex = (int) (index / 2L);
        int bitIndex = (int) (index % 2L);
        byte requiredByte = dataStoreBitMap[byteIndex];
        // Updated the byte.
        if (bitIndex == 1)
            requiredByte = (byte)((requiredByte & (byte)0b11110000) | (bitmap & (byte)0b00001111));
        else
            requiredByte = (byte)((requiredByte & (byte)0b00001111) | ((bitmap & (byte)0b00001111) << 4));
        dataStoreBitMap[byteIndex] = requiredByte;
        setDirtyFlag("DATA_STORE");
        writeToFile("DATA_STORE");
    }

    /**
     * Checks whether a block with the given index exists on disk or not.
     * @param index The index of the target block.
     * @return true if and only if the block has been allocated on disk.
     */
    public boolean isDataStoreIndexAllocated(long index){
        int byteIndex = (int) (index / 2L);
        int bitIndex = (int) (index % 2L);
        byte requiredByte = dataStoreBitMap[byteIndex];
        if (bitIndex == 0)
            return ((byte)(requiredByte & (byte)0b11110000)) == (byte)0b10010000;
        else
            return ((byte)(requiredByte & (byte)0b00001111)) == (byte)0b00001001;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // EXTENT_STORE METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns free extent index that can hold consecutive extent entries
     * @param totalEntries Number of consecutive entries / extent blocks needed
     * @return Appropriate index within the extent store to store the specified number of extent entries
     */
    public long getFreeIndexExtentStore(int totalEntries) {
        int runningFreeEntries = 0;
        long firstIndex = -1;
        for (int i = 0; i < extentStoreBitMap.length; i++){
            // If all indices in byte are used.
            if (extentStoreBitMap[i] == 0b01010101){
                firstIndex = -1;
                runningFreeEntries = 0;
                continue;
            }
            for (int j = 3; j > -1; j--){
                if (((extentStoreBitMap[i] >> (j * 2)) & 0b00000001) == 1){
                    if (firstIndex == -1){
                        firstIndex = (long)i * 4 + (3 - j);
                        runningFreeEntries = 1;
                    } else {
                        runningFreeEntries++;
                    }
                    if (runningFreeEntries == totalEntries)
                        return firstIndex;
                } else {
                    firstIndex = -1;
                    runningFreeEntries = 0;
                }
            }
        }
        // The requirement could not be met with the current bitmap.
        // Extending the bitmap.
        int extendFactor = totalEntries / 16;
        byte[] arr = new byte[extentStoreBitMap.length + (16 * (extendFactor + 1))];
        System.arraycopy(extentStoreBitMap, 0, arr, 0, extentStoreBitMap.length);
        for (int i = extentStoreBitMap.length; i< arr.length; i++){
            arr[i] = (byte) 0b10101010;
        }
        extentStoreBitMap = arr;
        setDirtyFlag("EXTENT_STORE");
        writeToFile("EXTENT_STORE");
        if (firstIndex == -1)
            return extentStoreBitMap.length;
        else
            return firstIndex;
    }
    /**
     * Checks whether a block with the given index exists on disk or not.
     * @param index The index of the target block.
     * @return true if and only if the block has been allocated on disk.
     */
    public boolean isExtentStoreIndexAllocated(long index, int totalEntries){
        // NEEDS TO BE FIXED. Should return with respect to the last index of the entries. index + totalEntries
        int byteIndex = (int) (index / 4L);
        int bitIndex = (int) (index % 4L);
        byte requiredByte = dataStoreBitMap[byteIndex];
        return (((byte)(requiredByte >> 2 * (4 - bitIndex))) & 0b00000010) == 0;
    }

    /**
     * @return index of the last allocated extent.
     */
    public long extentStoreGetLastAllocated(){
        // IMPLEMENT
        return 0;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FILE UTILITIES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void writeToFile(String storeName){
        byte[] byteArr;
        String filename = switch (storeName) {
            case "DIRECTORY_STORE" -> {
                byteArr = directoryStoreBitMap;
                yield "directory-store.bitmap";
            }
            case "EXTENT_STORE" -> {
                byteArr = extentStoreBitMap;
                yield "directory-store.bitmap";
            }
            case "INODE_STORE" -> {
                byteArr = iNodeStoreBitMap;
                yield "directory-store.bitmap";
            }
            case "DATA_STORE" -> {
                byteArr = dataStoreBitMap;
                yield "data-store.bitmap";
            }
            case "THUMBNAIL_STORE" -> {
                byteArr = thumbnailStoreBitMap;
                yield "thumbnail-store.bitmap";
            }
            case "ATTRIBUTE_STORE" -> {
                byteArr = attributeStoreBitMap;
                yield "attribute-store.bitmap";
            }
            default -> throw new IllegalArgumentException("Invalid Store Name");
        };
        try {
            FileOutputStream fout = new FileOutputStream(parentPath + "/" + filename);
            fout.write(byteArr);
            fout.close();
        } catch (FileNotFoundException e){
            throw new RuntimeException("Unexpected Error Occurred. Bitmap File Not Found." + e.getMessage());
        } catch (IOException e){
            throw new RuntimeException("Unexpected Error Occurred. Unable to write to bitmap file." + e.getMessage());
        }
        resetDirtyFlag(storeName);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FLAGS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * This method resets the dirtyFlags (sets them to 0)
     */
    private void resetDirtyFlags(){
        dirtyFlags = 0;
    }
    private void setDirtyFlag(String storeName){
        switch (storeName){
            case "DIRECTORY_STORE":
                dirtyFlags = (byte)(dirtyFlags | 0b00000001);
                break;
            case "EXTENT_STORE":
                dirtyFlags = (byte)(dirtyFlags | 0b00000010);
                break;
            case "INODE_STORE":
                dirtyFlags = (byte)(dirtyFlags | 0b00000100);
                break;
            case "DATA_STORE":
                dirtyFlags = (byte)(dirtyFlags | 0b00001000);
                break;
            case "THUMBNAIL_STORE":
                dirtyFlags = (byte)(dirtyFlags | 0b00010000);
                break;
            case "ATTRIBUTE_STORE":
                dirtyFlags = (byte)(dirtyFlags | 0b00100000);
                break;
            default:
                throw new RuntimeException("Invalid Store Name");
        }
    }

    private void resetDirtyFlag(String storeName){
        switch (storeName){
            case "DIRECTORY_STORE":
                dirtyFlags = (byte)(dirtyFlags & ~0b00000001);
                break;
            case "EXTENT_STORE":
                dirtyFlags = (byte)(dirtyFlags & ~0b00000010);
                break;
            case "INODE_STORE":
                dirtyFlags = (byte)(dirtyFlags & ~0b00000100);
                break;
            case "DATA_STORE":
                dirtyFlags = (byte)(dirtyFlags & ~0b00001000);
                break;
            case "THUMBNAIL_STORE":
                dirtyFlags = (byte)(dirtyFlags & ~0b00010000);
                break;
            case "ATTRIBUTE_STORE":
                dirtyFlags = (byte)(dirtyFlags & ~0b00100000);
                break;
            default:
                throw new RuntimeException("Invalid Store Name");
        }
    }
    private boolean getDirtyFlag(String storeName) {
        return switch (storeName) {
            case "DIRECTORY_STORE" -> (dirtyFlags & 0b00000001) != 0;
            case "EXTENT_STORE" -> (dirtyFlags & 0b00000010) != 0;
            case "INODE_STORE" -> (dirtyFlags & 0b00000100) != 0;
            case "DATA_STORE" -> (dirtyFlags & 0b00001000) != 0;
            case "THUMBNAIL_STORE" -> (dirtyFlags & 0b00010000) != 0;
            case "ATTRIBUTE_STORE" -> (dirtyFlags & 0b00100000) != 0;
            default -> throw new RuntimeException("Invalid Store Name");
        };
    }
}
