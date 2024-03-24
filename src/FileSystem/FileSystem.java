package FileSystem;
import Constants.FLAGS;
import Constants.PRESET_FILE_NAMES;
import DiskUtility.*;
import Exceptions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;

/**
 * This class encompasses the entire virtual filesystem.
 */
public class FileSystem {
    private NodeTree dir;
    private SuperBlock superBlock;
    private Gateway gateway;
    byte flags;
    /**
     * The constructor for the FileSystem in private. FileSystem can only be created by using the appropriate methods.
     */
    private FileSystem(){
        flags = FLAGS.DEFAULT_FILE_SYSTEM;
    }

    /**
     * This method creates a new FileSystem and returns the FileSystem instance.
     * It will also call the init function to create all the requisite data structures and files.
     * @return A FileSystem instance
     */
    public static FileSystem createFileSystem(String fileSystemName) throws FileSystemCreationFailedException {
        FileSystem fs = new FileSystem();
        fs.superBlock = new SuperBlock(fileSystemName);
        try{
            fs.gateway = new Gateway(fs.superBlock, true);
            fs.dir = fs.gateway.getDirectoryStoreGateway().mount();
        } catch (Exception e){
            throw new FileSystemCreationFailedException("FileSystem Creation Failed.\n" + e.getMessage());
        }
        return fs;
    }

    /**
     * This method initializes takes the path of the parent directory of the filesystem files. It initializes all the
     * fields of the FileSystem.
     * @return true if and only if the init method was successful.
     */
    public static FileSystem mount(String path) throws Exception{
        return Gateway.mountFileSystem(new FileSystem(), path);
    }

    /**
     * This method creates a new directory at the specified path if the path exists.
     * Returns if and only if the operation is successful.
     * @param path The path of the new directory (exclusive of the new node)
     * @param name Name of the desired directory
     */
    public void createDirectory(String path, String name) throws Exception{
        try {
            Node node = dir.addNode(path, name, gateway.getDirectoryStoreGateway());
            gateway.addNode(node);
        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Unable to create directory." + e.getMessage());
        }
    }

    public void ls(String path){
        try {
            LinkedList<Node> nodes = dir.ls(path, gateway.getDirectoryStoreGateway());
            if (nodes == null)
                return;
            for (Node node : nodes) {
                System.out.println(node.getName());
            }
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
    /**
     * This method creates a new empty file at the specified path if the path exists.
     * @param path The path of the new directory (inclusive of the file name)
     * @return true if and only if the operation is successful.
     */
    public boolean createFile(String path){
        // IMPLEMENT
        return false;
    }

    /**
     * This method imports a file from another filesystem to the specified path if the path exists.
     * This method does not deal with directories. A wrapper will need to be provided to distinguish between adding
     * directories and files.
     * @param file  The File to be added.
     * @return true if and only if the operation is successful.
     */
    public boolean addFile(File file){
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // MAKE A WAY TO FIND OUT WHERE THE FILE IS BEING ADDED. i.e. what is the parent node of the file.
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        try {
            INode inode = gateway.addFile(file);
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        /*
        STEPS:

            -> Create an inode entry.
            -> Place the data in the data-store and get the appropriate inode entry.
            -> Create a node and add the node to the directory tree
            -> Add the node to the directoryTree.

         */
        return false;
    }

    /**
     * This method provides a FileOutputStream to access the required file.
     * @param path The path of the required file
     * @return A FileOutputStream for the requested File.
     */
    public FileOutputStream openFile(String path){
        // IMPLEMENT
        return null;
    }

    /**
     * This method provides a Node to access the directory. It does not provide an actual stream of bytes of the data
     * within the Node.
     * @param path The path of the required directory
     * @return A Node object containing the directory
     */
    public Node openDirectory(String path){
        // IMPLEMENT
        return null;
    }

    /**
     * This method renames a file.
     * @param newPath The new path of the file (inclusive of the file name)
     * @param existingPath The existing path of the file (inclusive of the file name)
     * @return true if and only if the operation is successful
     */
    public boolean renameFile(String newPath, String existingPath){
        // IMPLEMENT
        return false;
    }

    /**
     * This method renames the directory. It also changes the pointers of all the nodes within the directory to point to
     * the correct new location.
     * @param newPath The new path of the file (inclusive of the file name)
     * @param existingPath The existing path of the file (inclusive of the file name)
     * @return true if and only if the operation is successful
     */
    public boolean renameDirectory(String newPath, String existingPath){
        // IMPLEMENT
        return false;
    }

    /**
     * This method copies a file.
     * @param newPath The path of the new copy of the file (inclusive of the file name)
     * @param existingPath The existing path of the file (inclusive of the file name)
     * @return true if and only if the operation is successful
     */
    public boolean copyFile(String newPath, String existingPath){
        // IMPLEMENT
        return false;
    }

    /**
     * This method copies the directory. It also copies all the existing iNodes.
     * @param newPath The new path of the file (inclusive of the file name)
     * @param existingPath The existing path of the file (inclusive of the file name)
     * @return true if and only if the operation is successful
     */
    public boolean copyDirectory(String newPath, String existingPath){
        // IMPLEMENT
        return false;
    }

    /**
     * This method is called internally whether a new directory or a new file is created. It creates the appropriate
     * INode entries.
     * @param path The path of the new directory (inclusive of the new node name)
     * @param isDirectory true if the node is a directory
     * @return true if and only if the operation is successful.
     */
    public boolean __createNode(String path, boolean isDirectory){
        // IMPLEMENT
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // THE CLASSES BELOW NEED REFINEMENT BEFORE IMPLEMENTATION      ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * This method is used to write to a file
     * @return The number of bytes remaining to write. Returns 0 if and only if all bytes have been successfully written.
     */
    public int write(){
        // IMPLEMENT
        return -1;
    }

    /**
     * This method is used to read from a file
     * @return The number of bytes remaining to write. Returns 0 if and only if all bytes have been successfully written.
     */
    public int read(){
        // IMPLEMENT
        return -1;
    }
    //////////////////////////
    // GETTERS AND SETTERS
    //////////////////////////
    public NodeTree getDir() {
        return dir;
    }

    public void setDir(NodeTree dir) {
        this.dir = dir;
    }

    public SuperBlock getSuperBlock() {
        return superBlock;
    }

    public void setSuperBlock(SuperBlock superBlock) {
        this.superBlock = superBlock;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }
}
