package DiskUtility;

import Constants.DATA_STORE_BLOCK_FRAME;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class handles all the interactions between the filesystem and the bitmap files.
 */
public class BitMapUtility {
    private final File baseFile;
    private byte[] directoryStoreBitMap;
    private byte[] extentStoreBitMap;
    private byte[] iNodeStoreBitMap;
    private byte[] dataStoreBitMap;
    private byte[] thumbnailStoreBitMap;
    BitMapUtility(File baseFile, boolean initialize) throws Exception{
        this.baseFile = baseFile;
        if (initialize){
            __createBitmaps();
        }else{
            __loadBitMaps();
        }
    }

    private long getLastAllocatedBlockIndex(String storeName) {
        byte[] bitmap = switch(storeName){
            case "DIRECTORY_STORE" -> directoryStoreBitMap;
            case "EXTENT_STORE" -> extentStoreBitMap;
            case "INODE_STORE" -> iNodeStoreBitMap;
            case "DATA_STORE" -> dataStoreBitMap;
            case "THUMBNAIL_STORE" -> thumbnailStoreBitMap;
            default -> throw new RuntimeException("Invalid Store Name");
        };
        // In case of Half Bitmap
        if (bitmap == dataStoreBitMap || bitmap == thumbnailStoreBitMap){
            for (int i = bitmap.length -1; i > -1; i--){
                if (bitmap[i] == (byte)0b10001000)
                    continue;
                if ((bitmap[i] & (byte)0b00001111) != 0b00001000)
                    return (i * 2L) + 1;
                return i * 2L;
            }
        } else {
            // In case of singular bitmap
            for (int i = bitmap.length -1; i > -1; i--){
                if (bitmap[i] == -1)
                    continue;
                for (int j = 0; j < 8; j++){
                    if (((bitmap[i] >> j) & 0b00000001) == 1)
                        return (i * 8L) + (7 - j);
                }
            }
        }
        // IMPLEMENT
        return 0;
    };

    ;
    BitMapUtility(File baseFile) throws Exception{
        this(baseFile, false);
    };
    /**
     * This byte denotes whether a particular bitmap is dirty i.e. different from the version on disk.
     * 0x000TDIED
     * T -> ThumbnailStore
     * D -> DataStore
     * I -> INodeStore
     * E -> ExtentStore
     * D -> DirectoryStore
     */
    private byte dirtyFlags = 0x00;
    /**
     * This method creates and initializes bitmap files for the file system.
     * @throws Exception In case an IOException arises with creating or writing to the files.
     */
    private void __createBitmaps() throws Exception{
        // Initializing Singular Bitmaps
        directoryStoreBitMap = __getEmptySingularBitmapBytes();
        extentStoreBitMap = __getEmptySingularBitmapBytes();
        iNodeStoreBitMap = __getEmptySingularBitmapBytes();
        // Initializing Half Bitmaps
        dataStoreBitMap = __getEmptyHalfBitmap();
        thumbnailStoreBitMap = __getEmptyHalfBitmap();
        for (Store store: Store.values()){
            String bitmapName = store.fileName + ".bitmap";
            File bitmapFile;
            FileOutputStream fout;
            try{
                bitmapFile = Gateway.createFileInBaseDirectory(baseFile, bitmapName);
                fout = new FileOutputStream(bitmapFile);
                if (store.bitmapType == BitmapType.Singular){
                    fout.write(__getEmptySingularBitmapBytes());
                } else if (store.bitmapType == BitmapType.Half){
                    fout.write(__getEmptyHalfBitmap());
                }
                fout.close();
            } catch (Exception e){
                throw new Exception("Error Creating Bitmap File: " + bitmapName + "\n" + e.getMessage());
            }
        }
    }

    private byte[] __getEmptyHalfBitmap() {
        byte[] halfBitmapBytes = new byte[2048];
        Arrays.fill(halfBitmapBytes, (byte) 0b10001000);
        return halfBitmapBytes;
    }

    private byte[] __getEmptySingularBitmapBytes() {
        return new byte[1024];
    }

    /**
     * This method loads all the bitmap files from storage to memory.
     */
    private void __loadBitMaps() throws Exception{
        if (dirtyFlags != 0)
            throw new RuntimeException("Cannot Load Bitmaps from Storage. Dirty Bitmaps in memory need to be written to storage first");
        for (Store store: Store.values()){
            String bitmapName = store.fileName + ".bitmap";
            byte[] array;
            File bitmapFile;
            FileInputStream fin;
            try{
                bitmapFile = Gateway.getFileInBaseDirectory(baseFile, bitmapName);
                fin = new FileInputStream(bitmapFile);
                array = fin.readAllBytes();
                fin.close();
            } catch (Exception e){
                throw new Exception("Unable to load bitmap: " + bitmapName + " --- " + e.getMessage());
            }
            switch(store){
                case Store.DataStore -> {
                    dataStoreBitMap = array;
                }
                case Store.DirectoryStore -> {
                    directoryStoreBitMap = array;
                }
                case Store.ExtentStore -> {
                    extentStoreBitMap = array;
                }
                case Store.INodeStore -> {
                    iNodeStoreBitMap = array;
                }
                case Store.ThumbnailStore -> {
                    thumbnailStoreBitMap = array;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Singular BITMAP METHODS
    //
    // Singular Bitmaps are single bit bitmaps. 1 bit represents a block/frame.
    // The following stores rely on Singular Bitmaps
    //  -> DIRECTORY_STORE
    //  -> EXTENT_STORE
    //  -> INODE_STORE
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected long getFreeIndexSingularBitmap(Store store) throws Exception{
        byte[] bitmap = switch (store) {
            case Store.DirectoryStore -> directoryStoreBitMap;
            case Store.ExtentStore -> extentStoreBitMap;
            case Store.INodeStore -> iNodeStoreBitMap;
            default -> throw new RuntimeException("Invalid Store Name for Singular Bitmap");
        };
        for (int i = 0; i < bitmap.length; i++){
            // All indices are allocated
            if (bitmap[i] == -1)
                continue;
            else {
                int bitIndex;
                for (int j = 7; j > -1; j--) {
                    if (((bitmap[i] >> j) & 1) == 0) {
                        bitIndex = 7 - j;
                        return (i * 8) + bitIndex;
                    }
                }
            }
        }
        // In case no free index exists, allocate more indices to the bitmap and return the first index in the newly
        // allocated indices.
        int length = bitmap.length;
        byte[] arr = new byte[length + 1024];
        System.arraycopy(bitmap, 0, arr, 0, length);
        switch (store) {
            case Store.DirectoryStore -> directoryStoreBitMap = arr;
            case Store.ExtentStore -> extentStoreBitMap = arr;
            case Store.INodeStore -> iNodeStoreBitMap = arr;
            default -> throw new RuntimeException("THIS CODE SHOULD NOT EXECUTE");
        };
        setDirtyFlag(store);
        writeToFile(store);
        return length * 8L;
    }

    protected void setIndexSingularBitmap(Store store, long index, boolean value) throws Exception{
        byte[] bitmap = switch (store) {
            case Store.DirectoryStore -> directoryStoreBitMap;
            case Store.ExtentStore -> extentStoreBitMap;
            case Store.INodeStore -> iNodeStoreBitMap;
            default -> throw new RuntimeException("Invalid Store Name for Singular Bitmap");
        };
        if (index > bitmap.length)
            throw new IndexOutOfBoundsException("Invalid Index For Bitmap");
        int byteIndex = (int)(index / 8L);
        int bitIndex = (int)(index % 8L);
        byte targetByte = bitmap[byteIndex];
        if (value)
            targetByte = (byte)(targetByte | (0x01 << 7 - bitIndex));
        else
            targetByte = (byte)(targetByte & ~(0x01 << 7 - bitIndex));
        bitmap[byteIndex] = targetByte;
        setDirtyFlag(store);
        try{
            writeToFile(store, byteIndex);
        } catch (Exception e){
            throw new Exception("Unable to Set Index: " + e.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Half BITMAP METHODS
    //
    // Half Bitmaps are bitmaps that take up half a byte. 4 bit represents a block/frame.
    // The following stores rely on Singular Bitmaps
    //  -> DATA_STORE
    //  -> THUMBNAIL_STORE
    //
    //  The 4 bits are divided into the following segments
    //  F | TQ | H | Q
    // Q set to 1 indicates that Quarter of the block is full
    // H set to 1 indicates that Half of the block is full (Q must also be set to 1 in this case)
    // TQ set to 1 indicates that Three Quarters of the block is full (H and Q must also be set to 1 in this case)
    // F set to 1 indicates that the entire block is full (given that all the other bits are also set to 1).
    //
    // SPECIAL CASES
    // Case 1:
    //      1 | 0 | 0 | 0  -> Represents an empty block that is also not allocated (does not exist on disk)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected long getFreeIndexHalfBitmap(Store store, long bytesToWrite) throws Exception{
        /*
        Basic:
            Return the first block that has free space.
            Currently, the bytesToWrite parameter is being ignored. In a more sophisticated implementation down the line,
            the bytesToWrite parameter will be utilized to decide which block to return.
        Optimal:
            Check the fileSize and provide a block index accordingly. Also take into consideration the fact that a file
            can be larger than the current blocks allocated and hence would require the allocation of more blocks.
            Ideally should return the smallest block available to satisfy the fileSize needs.
         */
        byte[] bitmap = switch (store) {
            case Store.DataStore -> dataStoreBitMap;
            case Store.ThumbnailStore -> thumbnailStoreBitMap;
            default -> throw new RuntimeException("Invalid Store Name for Half Bitmap");
        };
        for (int i = 0; i < bitmap.length; i++){
            byte dataStoreByte = bitmap[i];
            if (dataStoreByte == -1) {
                continue;
            }
            if ((dataStoreByte & 0b11110000) != 0b11110000) {
                return (long)i * 2L;
            }
            else {
                return (i * 2L) + 1;
            }
        }
        // If the code reaches this point then new blocks need to be allocated.
        int index = bitmap.length;
        byte[] arr = new byte[index + 2046];
        System.arraycopy(bitmap, 0, arr, 0, index);
        for (int i = index; i < arr.length; i++)
            arr[i] = (byte)0b10001000;
        switch (store) {
            case Store.DataStore -> dataStoreBitMap = arr;
            case Store.ThumbnailStore -> thumbnailStoreBitMap = arr;
            default -> throw new RuntimeException("THIS CODE SHOULD NOT EXECUTE");
        };
        setDirtyFlag(store);
        writeToFile(store);
        return index * 2L;
    }

    /**
     * Update the bitmap at the particular index
     * @param index index of the bitmap needed to be updated
     * @param bytesOccupied Number of bytes occupied by the index
     */
    protected void setIndexHalfBitmap(Store store, long index, short bytesOccupied) throws Exception{
        byte[] bitmap = switch (store) {
            case Store.DataStore -> dataStoreBitMap;
            case Store.ThumbnailStore -> thumbnailStoreBitMap;
            default -> throw new RuntimeException("Invalid Store Name for Half Bitmap");
        };
        byte newBitmap;
        int totalBytes = DATA_STORE_BLOCK_FRAME.DATA_SIZE;
        if (bytesOccupied == 0){
            newBitmap = (byte)0b00001000;
        } else if (bytesOccupied < totalBytes / 4){
            newBitmap = (byte)0b00000000;
        } else if (bytesOccupied < totalBytes / 2){
            newBitmap = (byte)0b00000001;
        } else if (bytesOccupied <  3 * (totalBytes / 4)){
            newBitmap = (byte)0b00000011;
        } else if (bytesOccupied < totalBytes){
            newBitmap = (byte)0b00000111;
        } else {
            newBitmap = (byte)0b00001111;
        }
        int byteIndex = (int) (index / 2L);
        int bitIndex = (int) (index % 2L);
        byte targetByte = dataStoreBitMap[byteIndex];
        // Update the byte.
        if (bitIndex == 1)
            targetByte = (byte)((targetByte & (byte)0b11110000) | (newBitmap & (byte)0b00001111));
        else
            targetByte = (byte)((targetByte & (byte)0b00001111) | ((newBitmap & (byte)0b00001111) << 4));
        bitmap[byteIndex] = targetByte;
        setDirtyFlag(store);
        try{
            writeToFile(store, byteIndex);
        } catch (Exception e){
            throw new Exception("Unable to Set Index: " + e.getMessage());
        }
    }

    protected boolean isIndexOccupiedHalfBitmap(Store store, long index){
        byte[] bitmap = switch (store) {
            case Store.DataStore -> dataStoreBitMap;
            case Store.ThumbnailStore -> thumbnailStoreBitMap;
            default -> throw new RuntimeException("Invalid Store Name for Half Bitmap");
        };
        int byteIndex = (int) (index / 2L);
        int bitIndex = (int) (index % 2L);
        byte requiredByte = bitmap[byteIndex];
        if (bitIndex == 0)
            return !(((byte)(requiredByte & (byte)0b11110000)) == (byte)0b10000000);
        else
            return !(((byte)(requiredByte & (byte)0b00001111)) == (byte)0b00001000);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // AUXILIARY METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected long getFreeIndexDirectoryStore() throws Exception{
        return getFreeIndexSingularBitmap(Store.DirectoryStore);
    }

    protected void setIndexDirectoryStore(long index, boolean value) throws Exception {
        setIndexSingularBitmap(Store.DirectoryStore, index, value);
    }

    protected long getFreeIndexExtentStore() throws Exception{
        return getFreeIndexSingularBitmap(Store.ExtentStore);
    }

    protected void setIndexExtentStore(long index, boolean value) throws Exception{
        setIndexSingularBitmap(Store.ExtentStore, index, value);
    }

    protected long getFreeIndexINodeStore() throws Exception{
        return getFreeIndexSingularBitmap(Store.INodeStore);
    }

    protected void setIndexINodeStore(long index, boolean value) throws Exception{
        setIndexSingularBitmap(Store.INodeStore, index, value);
    }
    protected long getFreeIndexDataStore(long bytesToWrite) throws Exception{
        return getFreeIndexHalfBitmap(Store.DataStore, bytesToWrite);
    }
    protected void setIndexDataStore(long index, short bytesOccupied) throws Exception{
        setIndexHalfBitmap(Store.DataStore, index, bytesOccupied);
    }

    protected boolean isIndexOccupiedDataStore(long index){
        return isIndexOccupiedHalfBitmap(Store.DataStore, index);
    }
    protected long getFreeIndexThumbnailStore(long bytesToWrite) throws Exception{
        return getFreeIndexHalfBitmap(Store.ThumbnailStore, bytesToWrite);
    }
    protected void setIndexThumbnailStore(long index, short bytesOccupied) throws Exception{
        setIndexHalfBitmap(Store.ThumbnailStore, index, bytesOccupied);
    }
    protected boolean isIndexOccupiedThumbnailStore(long index){
        return isIndexOccupiedHalfBitmap(Store.ThumbnailStore, index);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FILE UTILITIES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void writeToFile(Store store) throws Exception{
        byte[] byteArr = _getByteArray(store);
        String bitmapName = store.fileName + ".bitmap";
        try {
            File bitmapFile = Gateway.getFileInBaseDirectory(baseFile, bitmapName);
            FileOutputStream fout = new FileOutputStream(bitmapFile);
            fout.write(byteArr);
            fout.close();
        } catch (Exception e){
            throw new Exception("Unable To Write to Bitmap File: " + bitmapName + " || " + e.getMessage());
        }
        resetDirtyFlag(store);
    }

    private void writeToFile(Store store, long index) throws Exception {
        byte[] byteArr = _getByteArray(store);
        String bitmapName = store.fileName + ".bitmap";
        try {
            File bitmapFile = Gateway.getFileInBaseDirectory(baseFile, bitmapName);
            RandomAccessFile fout = new RandomAccessFile(bitmapFile, "rw");
            fout.seek(index);
            fout.write(byteArr[(int)index]);
            fout.close();
        } catch (Exception e){
            throw new Exception("Unable To Write to Bitmap File: " + bitmapName + " || " + e.getMessage());
        }
        resetDirtyFlag(store);
    }

    private byte[] _getByteArray(Store store) throws Exception{
        switch (store){
            case Store.DataStore -> {
                return dataStoreBitMap;
            }
            case Store.DirectoryStore -> {
                return directoryStoreBitMap;
            }
            case Store.ExtentStore -> {
                return extentStoreBitMap;
            }
            case Store.INodeStore -> {
                return iNodeStoreBitMap;
            }
            case Store.ThumbnailStore -> {
                return thumbnailStoreBitMap;
            }
            default -> {
                throw new Exception("Invalid Store");
            }
        }
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

    private void setDirtyFlag(Store store){
        switch (store){
            case Store.DirectoryStore:
                dirtyFlags = (byte)(dirtyFlags | 0b00000001);
                break;
            case Store.ExtentStore:
                dirtyFlags = (byte)(dirtyFlags | 0b00000010);
                break;
            case Store.INodeStore:
                dirtyFlags = (byte)(dirtyFlags | 0b00000100);
                break;
            case Store.DataStore:
                dirtyFlags = (byte)(dirtyFlags | 0b00001000);
                break;
            case Store.ThumbnailStore:
                dirtyFlags = (byte)(dirtyFlags | 0b00010000);
                break;
            default:
                throw new RuntimeException("Invalid Store Name");
        }
    }

    private void resetDirtyFlag(Store store){
        switch (store){
            case Store.DirectoryStore:
                dirtyFlags = (byte)(dirtyFlags & ~0b00000001);
                break;
            case Store.ExtentStore:
                dirtyFlags = (byte)(dirtyFlags & ~0b00000010);
                break;
            case Store.INodeStore:
                dirtyFlags = (byte)(dirtyFlags & ~0b00000100);
                break;
            case Store.DataStore:
                dirtyFlags = (byte)(dirtyFlags & ~0b00001000);
                break;
            case Store.ThumbnailStore:
                dirtyFlags = (byte)(dirtyFlags & ~0b00010000);
                break;
            default:
                throw new RuntimeException("Invalid Store Name");
        }
    }
    private boolean getDirtyFlag(Store store) {
        return switch (store) {
            case Store.DirectoryStore -> (dirtyFlags & 0b00000001) != 0;
            case Store.ExtentStore -> (dirtyFlags & 0b00000010) != 0;
            case Store.INodeStore -> (dirtyFlags & 0b00000100) != 0;
            case Store.DataStore -> (dirtyFlags & 0b00001000) != 0;
            case Store.ThumbnailStore -> (dirtyFlags & 0b00010000) != 0;
            default -> throw new RuntimeException("Invalid Store Name");
        };
    }
}
