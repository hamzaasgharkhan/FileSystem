package FileSystem;

import Exceptions.INodeNotFoundException;
import Utilities.BinaryUtilities;

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
 * Magic Value                 -       4 bytes                                 || Starting Index: 0
 *     iNodeAddress                -       8 bytes                                 || Starting Index: 4
 *     parentINodeAddress          -       8 bytes                                 || Starting Index: 12
 *     size                        -       8 bytes                                 || Starting Index: 20
 *     flags                       -       1 byte                                  || Starting Index: 28
 *     creationTime                -       8 bytes                                 || Starting Index: 29
 *     lastModifiedTime            -       8 bytes                                 || Starting Index: 37
 *     Magic Value                 -       4 bytes                                 || Starting Index: 45
 *     extentStoreAddress          -       8 bytes                                 || Starting Index: 49
 *     extentCount                 -       8 bytes                                 || Starting Index: 57
 *     thumbnailStoreAddress       -       8 bytes                                 || Starting Index: 65
 *     Magic Value
 */
public class INode {
    private long iNodeAddress;
    private long parentINodeAddress;
    private long iNodeSize;
    private byte flags;
    private long creationTime;
    private long lastModifiedTime;
    private long extentStoreAddress;
    private long extentCount;
    private long thumbnailStoreAddress;
    public INode(){

    }

    /**
     * This method checks the flags to determine whether the calling iNode is a directory.
     * @return true if the calling iNode is a directory.
     */
    public boolean isDirectory(){
        // IMPLEMENT LATER
        return true;
    }
    /**
     * This static method takes an iNodeAddress and retrieves the iNode from the iNodeStore on disk.
     * @param iNodeAddress The address of the requested iNode
     * @return The requested iNode
     * @throws INodeNotFoundException Throws an exception in case of an invalid iNode Address.
     */
    public static INode getINode(long iNodeAddress){
        // IMPLEMENT LATER
        return null;
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
            case "PARENT_INODE_ADDRESS" -> BinaryUtilities.convertLongToBytes(parentINodeAddress);
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
}