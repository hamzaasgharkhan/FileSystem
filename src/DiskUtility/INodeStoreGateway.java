package DiskUtility;

import Constants.INODE_FRAME;
import Constants.VALUES;
import Exceptions.INodeFileNotFoundException;
import FileSystem.INode;
import FileSystem.SuperBlock;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * This class serves as a gateway between the iNodeStore File and the rest of the filesystem.
 */
public class INodeStoreGateway {
    // Denotes the extentFrame
    File iNodeFile;
    final BitMapUtility bitMapUtility;
    public INodeStoreGateway(String path, BitMapUtility bitMapUtility) throws Exception {
        File file = new File(path);
        if (!file.exists())
            throw new Exception("Directory Store File Does Not Exist.");
        this.bitMapUtility = bitMapUtility;
        iNodeFile = file;
    }

    public INode addNode(File file) throws Exception{

        // IMPLEMENT
        return null;
    }


    /**
     * This method creates a new iNode Frame within the INodeFile and places the provided iNode into that frame.
     * @param iNode The INode to be added to the file.
     */
    public void addINode(INode iNode){
        byte[] iNodeFrame = getINodeFrame(iNode);
        // IMPLEMENT
    }

    /**
     * This method takes the iNodeAddress and returns the relevant iNode.
     * @param iNodeAddress the iNodeAddress of the required iNode
     * @return The required iNode object
     */
    public INode getINode(long iNodeAddress){
        // IMPLEMENT
        return null;
    }

    /**
     * This method takes an iNode object and an iNodeAddress. The iNode at the particular address is updated to the new
     * iNode object.
     * @param iNode The updated iNode.
     * @param iNodeAddress  Target iNodeAddress
     */
    public void updateINode(INode iNode, long iNodeAddress){
        // IMPLEMENT
    }
    public byte[] getINodeFrame(INode iNode){
        byte[] byteArray = new byte[INODE_FRAME.INODE_FRAME_SIZE];
        System.arraycopy(VALUES.MAGIC_VALUE_BYTES, 0, byteArray, INODE_FRAME.MAGIC_VALUE_1_INDEX, 4);
        System.arraycopy(VALUES.MAGIC_VALUE_BYTES, 0, byteArray, INODE_FRAME.MAGIC_VALUE_2_INDEX, 4);
        System.arraycopy(VALUES.MAGIC_VALUE_BYTES, 0, byteArray, INODE_FRAME.MAGIC_VALUE_3_INDEX, 4);
        System.arraycopy(iNode.getFieldBytes("INODE_ADDRESS"), 0, byteArray, INODE_FRAME.INODE_ADDRESS_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("PARENT_INODE_ADDRESS"), 0, byteArray, INODE_FRAME.PARENT_INODE_ADDRESS_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("SIZE"), 0, byteArray, INODE_FRAME.SIZE_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("FLAGS"), 0, byteArray, INODE_FRAME.FLAGS_INDEX, 1);
        System.arraycopy(iNode.getFieldBytes("CREATION_TIME"), 0, byteArray, INODE_FRAME.CREATION_TIME_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("LAST_MODIFIED_TIME"), 0, byteArray, INODE_FRAME.LAST_MODIFIED_TIME_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("THUMBNAIL_STORE_ADDRESS"), 0, byteArray, INODE_FRAME.THUMBNAIL_STORE_ADDRESS_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("EXTENT_STORE_ADDRESS"), 0, byteArray, INODE_FRAME.EXTENT_STORE_ADDRESS_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("EXTENT_COUNT"), 0, byteArray, INODE_FRAME.EXTENT_COUNT_INDEX, 8);
        return byteArray;
    }
}
