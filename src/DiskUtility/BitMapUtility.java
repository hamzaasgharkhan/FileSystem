package DiskUtility;

import Constants.DATA_STORE_BLOCK_FRAME;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * This class handles all the interactions between the filesystem and the bitmap files.
 */
public class BitMapUtility {
    private final Path basePath;
    private byte[] directoryStoreBitMap;
    private byte[] extentStoreBitMap;
    private byte[] iNodeStoreBitMap;
    private byte[] dataStoreBitMap;
    private byte[] thumbnailStoreBitMap;
    BitMapUtility(Path path, boolean initialize) throws Exception{
        basePath = path;
        if (initialize){
            __createBitmaps();
        }else{
            __loadBitMaps(path);
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
    BitMapUtility(Path path) throws Exception{
        this(path, false);
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
        String[] bitmapNames = {
                // Singular Bitmaps
                "directory-store.bitmap",
                "extent-store.bitmap",
                "inode-store.bitmap",
                // Half Bitmaps
                "data-store.bitmap",
                "thumbnail-store.bitmap"
        };
        // Initializing Quarter Bitmaps
        directoryStoreBitMap = __getEmptySingularBitmapBytes();
        extentStoreBitMap = __getEmptySingularBitmapBytes();
        iNodeStoreBitMap = __getEmptySingularBitmapBytes();
        for (int i = 0; i < 3; i++){
            try {
                Files.write(basePath.resolve(bitmapNames[i]), directoryStoreBitMap);
            } catch (IOException e){
                throw new Exception("Error creating bitmap file: " + bitmapNames[i]);
            }
        }
        // Initializing Half Bitmaps
        dataStoreBitMap = __getEmptyHalfBitmap();
        thumbnailStoreBitMap = __getEmptyHalfBitmap();
        for (int i = 3; i < bitmapNames.length; i++){
            try {
                Files.write(basePath.resolve(bitmapNames[i]), dataStoreBitMap);
            } catch (IOException e){
                throw new Exception("Error creating bitmap file: " + bitmapNames[i]);
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
     * @param path Path of the fileSystem.
     */
    private void __loadBitMaps(Path path) throws Exception{
        if (dirtyFlags != 0)
            throw new RuntimeException("Cannot Load Bitmaps from Storage. Dirty Bitmaps in memory need to be written to storage first");
        byte[][] arrays = new byte[5][];
        String[] paths = {
                "directory-store.bitmap",
                "extent-store.bitmap",
                "inode-store.bitmap",
                "data-store.bitmap",
                "thumbnail-store.bitmap",
        };
        for (int i = 0; i < arrays.length; i++){
            try {
                FileInputStream fin = new FileInputStream(path.resolve(paths[i]).toFile());
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
    protected long getFreeIndexSingularBitmap(String storeName){
        byte[] bitmap = switch (storeName) {
            case "DIRECTORY_STORE" -> directoryStoreBitMap;
            case "EXTENT_STORE" -> extentStoreBitMap;
            case "INODE_STORE" -> iNodeStoreBitMap;
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
        switch (storeName) {
            case "DIRECTORY_STORE" -> directoryStoreBitMap = arr;
            case "EXTENT_STORE" -> extentStoreBitMap = arr;
            case "INODE_STORE" -> iNodeStoreBitMap = arr;
            default -> throw new RuntimeException("THIS CODE SHOULD NOT EXECUTE");
        };
        setDirtyFlag(storeName);
        writeToFile(storeName);
        return length * 8L;
    }

    protected void setIndexSingularBitmap(String storeName, long index, boolean value){
        byte[] bitmap = switch (storeName) {
            case "DIRECTORY_STORE" -> directoryStoreBitMap;
            case "EXTENT_STORE" -> extentStoreBitMap;
            case "INODE_STORE" -> iNodeStoreBitMap;
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
        setDirtyFlag(storeName);
        writeToFile(storeName, byteIndex);
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

    protected long getFreeIndexHalfBitmap(String storeName, long bytesToWrite){
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
        byte[] bitmap = switch (storeName) {
            case "DATA_STORE" -> dataStoreBitMap;
            case "THUMBNAIL_STORE" -> thumbnailStoreBitMap;
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
        switch (storeName) {
            case "DATA_STORE" -> dataStoreBitMap = arr;
            case "THUMBNAIL_STORE" -> thumbnailStoreBitMap = arr;
            default -> throw new RuntimeException("THIS CODE SHOULD NOT EXECUTE");
        };
        setDirtyFlag(storeName);
        writeToFile(storeName);
        return index * 2L;
    }

    /**
     * Update the bitmap at the particular index
     * @param index index of the bitmap needed to be updated
     * @param bytesOccupied Number of bytes occupied by the index
     */
    protected void setIndexHalfBitmap(String storeName, long index, short bytesOccupied){
        byte[] bitmap = switch (storeName) {
            case "DATA_STORE" -> dataStoreBitMap;
            case "THUMBNAIL_STORE" -> thumbnailStoreBitMap;
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
        setDirtyFlag(storeName);
        writeToFile(storeName, byteIndex);
    }

    protected boolean isIndexOccupiedHalfBitmap(String storeName, long index){
        byte[] bitmap = switch (storeName) {
            case "DATA_STORE" -> dataStoreBitMap;
            case "THUMBNAIL_STORE" -> thumbnailStoreBitMap;
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
    protected long getFreeIndexDirectoryStore(){
        return getFreeIndexSingularBitmap("DIRECTORY_STORE");
    }

    protected void setIndexDirectoryStore(long index, boolean value){
        setIndexSingularBitmap("DIRECTORY_STORE", index, value);
    }

    protected long getFreeIndexExtentStore(){
        return getFreeIndexSingularBitmap("EXTENT_STORE");
    }

    protected void setIndexExtentStore(long index, boolean value){
        setIndexSingularBitmap("EXTENT_STORE", index, value);
    }

    protected long getFreeIndexINodeStore(){
        return getFreeIndexSingularBitmap("INODE_STORE");
    }

    protected void setIndexINodeStore(long index, boolean value){
        setIndexSingularBitmap("INODE_STORE", index, value);
    }
    protected long getFreeIndexDataStore(long bytesToWrite){
        return getFreeIndexHalfBitmap("DATA_STORE", bytesToWrite);
    }
    protected void setIndexDataStore(long index, short bytesOccupied){
        setIndexHalfBitmap("DATA_STORE", index, bytesOccupied);
    }

    protected boolean isIndexOccupiedDataStore(long index){
        return isIndexOccupiedHalfBitmap("DATA_STORE", index);
    }
    protected long getFreeIndexThumbnailStore(long bytesToWrite){
        return getFreeIndexHalfBitmap("THUMBNAIL_STORE", bytesToWrite);
    }
    protected void setIndexThumbnailStore(long index, short bytesOccupied){
        setIndexHalfBitmap("THUMBNAIL_STORE", index, bytesOccupied);
    }
    protected boolean isIndexOccupiedThumbnailStore(long index){
        return isIndexOccupiedHalfBitmap("THUMBNAIL_STORE", index);
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
            default -> throw new IllegalArgumentException("Invalid Store Name");
        };
        try {
            FileOutputStream fout = new FileOutputStream(basePath.resolve(filename).toFile());
            fout.write(byteArr);
            fout.close();
        } catch (FileNotFoundException e){
            throw new RuntimeException("Unexpected Error Occurred. Bitmap File Not Found." + e.getMessage());
        } catch (IOException e){
            throw new RuntimeException("Unexpected Error Occurred. Unable to write to bitmap file." + e.getMessage());
        }
        resetDirtyFlag(storeName);
    }

    public void writeToFile(String storeName, long index){
        byte[] byteArr;
        String filename = switch (storeName) {
            case "DIRECTORY_STORE" -> {
                byteArr = directoryStoreBitMap;
                yield "directory-store.bitmap";
            }
            case "EXTENT_STORE" -> {
                byteArr = extentStoreBitMap;
                yield "extent-store.bitmap";
            }
            case "INODE_STORE" -> {
                byteArr = iNodeStoreBitMap;
                yield "inode-store.bitmap";
            }
            case "DATA_STORE" -> {
                byteArr = dataStoreBitMap;
                yield "data-store.bitmap";
            }
            case "THUMBNAIL_STORE" -> {
                byteArr = thumbnailStoreBitMap;
                yield "thumbnail-store.bitmap";
            }
            default -> throw new IllegalArgumentException("Invalid Store Name");
        };
        try {
            RandomAccessFile fout = new RandomAccessFile(basePath.resolve(filename).toFile(), "rw");
            fout.seek(index);
            fout.write(byteArr[(int)index]);
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
            default -> throw new RuntimeException("Invalid Store Name");
        };
    }
}
