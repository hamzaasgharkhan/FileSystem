package FileSystem;

import Exceptions.INodeNotFoundException;
import Utilities.BinaryUtilities;
import Constants.FLAGS;

/**
 * This class provides the logical representation for inodes.
 * Each iNode contains the following:
 * iNodeAddress         :   The Address used to locate the file within the iNodeStore. Each objects points to itself for
 *                          easy backtracking.
 * iNodeSize            :   Contains the size of the actual iNode on disk.
 * flags                :   Contains flags associated with the file.
 * extentStoreAddress   :   Contains the relevant address in the extentStore to retrieve the extent pointers.
 * extentCount          :   Contains the number of extent pointers.
 *
 *
 md5 checksum                -       16 bytes                                || Starting Index: 0
 iNodeAddress                -       8 bytes                                 || Starting Index: 16
 size                        -       8 bytes                                 || Starting Index: 24
 flags                       -       1 byte                                  || Starting Index: 32
 creationTime                -       8 bytes                                 || Starting Index: 33
 lastModifiedTime            -       8 bytes                                 || Starting Index: 41
 extentStoreAddress          -       8 bytes                                 || Starting Index: 49
 extentCount                 -       8 bytes                                 || Starting Index: 57
 thumbnailStoreAddress       -       8 bytes                                 || Starting Index: 65
 Size: 73 bytes

 */
public class INode {
    private long iNodeAddress;
    private long iNodeSize;
    private byte flags;
    private long creationTime;
    private long lastModifiedTime;
    private long extentStoreAddress;
    private long extentCount;
    private long thumbnailStoreAddress;

    public INode(){
        this.flags = FLAGS.DEFAULT_INODE;
    }

    /**
     * This method returns the byte Array representation of any field of the SuperBlock
     * @param field The name of the field. Can be one of the following:
     *              INODE_ADDRESS
     *              PARENT_INODE_ADDRESS
     *              SIZE
     *              FLAGS
     *              CREATION_TIME
     *              LAST_MODIFIED_TIME
     *              EXTENT_STORE_ADDRESS
     *              EXTENT_COUNT
     *              THUMBNAIL_STORE_ADDRESS
     * @return A byte array containing the desired field
     */
    public byte[] getFieldBytes(String field){
        return switch (field) {
            case "INODE_ADDRESS" -> BinaryUtilities.convertLongToBytes(iNodeAddress);
            case "SIZE" -> BinaryUtilities.convertLongToBytes(iNodeSize);
            case "FLAGS" -> new byte[]{flags};
            case "CREATION_TIME" -> BinaryUtilities.convertLongToBytes(creationTime);
            case "LAST_MODIFIED_TIME" -> BinaryUtilities.convertLongToBytes(lastModifiedTime);
            case "EXTENT_STORE_ADDRESS" -> BinaryUtilities.convertLongToBytes(extentStoreAddress);
            case "EXTENT_COUNT" -> BinaryUtilities.convertLongToBytes(extentCount);
            case "THUMBNAIL_STORE_ADDRESS" -> BinaryUtilities.convertLongToBytes(thumbnailStoreAddress);
            default -> throw new IllegalArgumentException("No such field exists");
        };
    }

    public long getiNodeAddress() {
        return iNodeAddress;
    }

    public void setiNodeAddress(long iNodeAddress) {
        this.iNodeAddress = iNodeAddress;
    }

    public long getiNodeSize() {
        return iNodeSize;
    }

    public void setiNodeSize(long iNodeSize) {
        this.iNodeSize = iNodeSize;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public long getExtentStoreAddress() {
        return extentStoreAddress;
    }

    public void setExtentStoreAddress(long extentStoreAddress) {
        this.extentStoreAddress = extentStoreAddress;
    }

    public long getExtentCount() {
        return extentCount;
    }

    public void setExtentCount(long extentCount) {
        this.extentCount = extentCount;
    }

    public long getThumbnailStoreAddress() {
        return thumbnailStoreAddress;
    }

    public void setThumbnailStoreAddress(long thumbnailStoreAddress) {
        this.thumbnailStoreAddress = thumbnailStoreAddress;
    }

}