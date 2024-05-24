package DiskUtility;

import Constants.FLAGS;
import Constants.INODE_STORE_FRAME;
import FileSystem.INode;
import Utilities.BinaryUtilities;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
/**
 * This class serves as a gateway between the iNodeStore File and the rest of the filesystem.
 */
public class INodeStoreGateway {
    // Denotes the extentFrame
    private File iNodeFile;
    private final BitMapUtility bitMapUtility;
    private final SecretKey key;
    public INodeStoreGateway(Path path, BitMapUtility bitMapUtility, SecretKey key) throws Exception {
        File file = path.resolve("inode-store").toFile();
        if (!file.exists())
            throw new Exception("Directory Store File Does Not Exist.");
        this.bitMapUtility = bitMapUtility;
        this.key = key;
        iNodeFile = file;
    }

    public INode addNode(Path path, long[] extentDetails, long thumbnailStoreAddress) throws Exception{
        long creationTime;
        long lastModifiedTime;
        long iNodeAddress;
        long iNodeSize;
        INode iNode;
        RandomAccessFile fin;
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            creationTime = attributes.creationTime().toMillis();
            lastModifiedTime = attributes.lastModifiedTime().toMillis();
            iNodeSize = attributes.size();
        } catch (IOException e){
            throw new Exception("Unable to access the attributes of the requested file." + e.getMessage());
        }
        iNodeAddress = bitMapUtility.getFreeIndexINodeStore();
        iNode = new INode();
        iNode.setiNodeAddress(iNodeAddress);
        iNode.setiNodeSize(iNodeSize);
        iNode.setCreationTime(creationTime);
        iNode.setLastModifiedTime(lastModifiedTime);
        iNode.setExtentStoreAddress(extentDetails[0]);
        iNode.setExtentCount(extentDetails[1]);
        iNode.setThumbnailStoreAddress(thumbnailStoreAddress);
        iNode.setFlags(FLAGS.DEFAULT_INODE);
        try {
            fin = new RandomAccessFile(iNodeFile, "rw");
        } catch (FileNotFoundException e){
            throw new Exception("INODE_STORE File Not Found." + e.getMessage());
        }
        try {
            fin.seek(iNodeAddress * INODE_STORE_FRAME.FULL_SIZE);
        } catch (IOException e){
            throw new Exception("INODE_STORE Unable to seek. IOException thrown." + e.getMessage());
        }
        try{
            byte[] byteArray = __getINodeFrame(iNode);
            byteArray = Crypto.encryptBlock(byteArray, key, INODE_STORE_FRAME.SIZE);
            fin.write(byteArray);
        } catch (IOException e){
            throw new Exception("Unable to write new INODE_FRAME to the INODE_STORE." + e.getMessage());
        }
        try {
            fin.close();
        } catch (IOException e){
            throw new Exception("Unable to close INODE_STORE file." + e.getMessage());
        }
        bitMapUtility.setIndexINodeStore(iNodeAddress, true);
        return iNode;
    }

    /**
     * This method takes the iNodeAddress and returns the relevant iNode.
     * @param iNodeAddress the iNodeAddress of the required iNode
     * @return The required iNode object
     */
    public INode getINode(long iNodeAddress) throws Exception{
        // IMPLEMENT
        RandomAccessFile fin;
        byte[] byteArray = new byte[INODE_STORE_FRAME.FULL_SIZE];
        try {
            fin = new RandomAccessFile(iNodeFile, "r");
        } catch (FileNotFoundException e){
            throw new Exception("INODE_STORE File Not Found." + e.getMessage());
        }
        try {
            fin.seek(iNodeAddress * INODE_STORE_FRAME.FULL_SIZE);
        } catch (IOException e){
            throw new Exception("INODE_STORE Unable to seek. IOException thrown." + e.getMessage());
        }
        try{
            fin.readFully(byteArray);
        } catch (IOException e){
            throw new Exception("Unable to read from INODE_STORE." + e.getMessage());
        }
        try {
            fin.close();
        } catch (IOException e){
            throw new Exception("Unable to close INODE_STORE file." + e.getMessage());
        }

        return __getINode(byteArray);
    }

    /**
     * This method takes an INodeAddress and removes the INode Entry from the FileSystem
     * @param iNodeAddress Target INode Address
     */
    public void removeINode(long iNodeAddress){
        // Remove the bitmap entry
        // Delete the entry in case if it is the last one.
        bitMapUtility.setIndexINodeStore(iNodeAddress, false);
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

    private byte[] __getINodeFrame(INode iNode){
        byte[] byteArray = new byte[INODE_STORE_FRAME.SIZE];
        System.arraycopy(iNode.getMD5Hash(), 0, byteArray, INODE_STORE_FRAME.MD5_HASH_INDEX, 16);
        System.arraycopy(iNode.getFieldBytes("INODE_ADDRESS"), 0, byteArray, INODE_STORE_FRAME.INODE_ADDRESS_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("SIZE"), 0, byteArray, INODE_STORE_FRAME.SIZE_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("FLAGS"), 0, byteArray, INODE_STORE_FRAME.FLAGS_INDEX, 1);
        System.arraycopy(iNode.getFieldBytes("CREATION_TIME"), 0, byteArray, INODE_STORE_FRAME.CREATION_TIME_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("LAST_MODIFIED_TIME"), 0, byteArray, INODE_STORE_FRAME.LAST_MODIFIED_TIME_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("THUMBNAIL_STORE_ADDRESS"), 0, byteArray, INODE_STORE_FRAME.THUMBNAIL_STORE_ADDRESS_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("EXTENT_STORE_ADDRESS"), 0, byteArray, INODE_STORE_FRAME.EXTENT_STORE_ADDRESS_INDEX, 8);
        System.arraycopy(iNode.getFieldBytes("EXTENT_COUNT"), 0, byteArray, INODE_STORE_FRAME.EXTENT_COUNT_INDEX, 8);
        return byteArray;
    }

    /**
     * This method takes a byteArray of an iNode and returns the iNode.
     * @param byteArray A byteArray containing the bytes of an iNodeFrame along with the encryption information
     * @return Target INode Object
     */
    private INode __getINode(byte[] byteArray) throws Exception{
        byteArray = Crypto.decryptBlock(byteArray, key, INODE_STORE_FRAME.SIZE);
        INode iNode = new INode();
        iNode.setiNodeAddress(BinaryUtilities.convertBytesToLong(byteArray, INODE_STORE_FRAME.INODE_ADDRESS_INDEX));
        iNode.setiNodeSize(BinaryUtilities.convertBytesToLong(byteArray, INODE_STORE_FRAME.SIZE_INDEX));
        iNode.setFlags(byteArray[INODE_STORE_FRAME.FLAGS_INDEX]);
        iNode.setExtentStoreAddress(BinaryUtilities.convertBytesToLong(byteArray, INODE_STORE_FRAME.EXTENT_STORE_ADDRESS_INDEX));
        iNode.setExtentCount(BinaryUtilities.convertBytesToLong(byteArray, INODE_STORE_FRAME.EXTENT_COUNT_INDEX));
        iNode.setCreationTime(BinaryUtilities.convertBytesToLong(byteArray, INODE_STORE_FRAME.CREATION_TIME_INDEX));
        iNode.setLastModifiedTime(BinaryUtilities.convertBytesToLong(byteArray, INODE_STORE_FRAME.LAST_MODIFIED_TIME_INDEX));
        iNode.setThumbnailStoreAddress(BinaryUtilities.convertBytesToLong(byteArray, INODE_STORE_FRAME.THUMBNAIL_STORE_ADDRESS_INDEX));
        return iNode;
    }
}
