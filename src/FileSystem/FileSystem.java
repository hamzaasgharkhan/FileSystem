package FileSystem;

import Constants.FLAGS;
import DiskUtility.Crypto;
import DiskUtility.CustomInputStream;
import DiskUtility.Gateway;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * This class encompasses the entire virtual filesystem.
 */
public class FileSystem {
    private NodeTree dir;
    private Gateway gateway;
    private byte flags;
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
    public static FileSystem createFileSystem(File baseFile, String fileSystemName, String password) throws Exception {
        FileSystem fs = new FileSystem();
        Crypto.init();
        SuperBlock superBlock = new SuperBlock(fileSystemName);
        if (!baseFile.isDirectory()){
            throw new Exception("FileSystem Creation Failed: Provided baseFile does not point to a directory");
        }
        File fileSystemBaseFile = new File(baseFile, fileSystemName);
        try{
            fs.gateway = new Gateway(fileSystemBaseFile, superBlock, password, true);
            fs.dir = new NodeTree();
            fs.__writeDirtyNodes();
        } catch (Exception e){
            throw new Exception("FileSystem Creation Failed.\n" + e.getMessage());
        }
        return fs;
    }

    /**
     * This method initializes takes the path of the root directory of the filesystem files. It initializes all the
     * fields of the FileSystem.
     * @return true if and only if the init method was successful.
     */
    public static FileSystem mount(File baseFile, String password) throws Exception{
        Crypto.init();
        FileSystem fs =  Gateway.mountFileSystem(new FileSystem(), baseFile, password);
        fs.gateway.readChildren(fs.dir.getRoot());
        return fs;
    }

    /**
     * This method creates a new directory at the specified path if the path exists.
     * Returns if and only if the operation is successful.
     * @param path The path of the new directory (exclusive of the new node)
     * @param name Name of the desired directory
     */
    public void createDirectory(String path, String name) throws Exception{
        try {
            dir.addNode(path, name, gateway.getDirectoryStoreGateway());
            __writeDirtyNodes();
        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Unable to create directory." + e.getMessage());
        }
    }

    public void createDirectory(Node parentNode, String name) throws Exception{
        dir.addNode(parentNode, name);
        __writeDirtyNodes();
    }

    public void ls(String path) throws Exception{
        try {
            LinkedList<Node> nodes = dir.ls(path, gateway.getDirectoryStoreGateway());
            if (nodes == null)
                return;
            for (Node node : nodes) {
                System.out.println(node.getName());
            }
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
    /**
     * This method imports a file from another filesystem to the specified path if the path exists.
     * This method does not deal with directories. A wrapper will need to be provided to distinguish between adding
     * directories and files.
     * @param file InputFile object containing the file
     * @return true if and only if the operation is successful.
     */
    public void addFile(InputFile file) throws Exception{
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // MAKE A WAY TO FIND OUT WHERE THE FILE IS BEING ADDED. i.e. what is the parent node of the file.
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /*
        STEPS:
            -> Place the data in the data-store and get the appropriate inode entry.
            -> Create an inode entry.
            -> Check the path of the added file. If the same parent directory exists within the directory structure,
            add it there. If the same parent directory does not exist within the directory structure, create the
            directory structure and then add the node to that path.
            -> Create a node and add the node to the directory tree
         */
        String parentPath = file.parentPath;
        if (file.fileInputStream == null)
            throw new Exception("File Does Not Exist");
        INode iNode = gateway.addFile(file);
        Node parentNode = dir.getOrCreatePath(parentPath, gateway.getDirectoryStoreGateway());
        Node node = dir.addNode(parentNode, file.name, iNode.getiNodeAddress());
        if (iNode.getThumbnailStoreAddress() != -1){
            node.setFlag(Node.HAS_THUMBNAIL_FLAG_MASK, true);
        }
        __writeDirtyNodes();
    }

    public void addFile(Node parentNode, InputFile file) throws Exception{
        if (file.fileInputStream == null)
            throw new Exception("File Does Not Exist");
        INode iNode = gateway.addFile(file);
        Node node = dir.addNode(parentNode, file.name, iNode.getiNodeAddress());
        if (iNode.getThumbnailStoreAddress() != -1){
            node.setFlag(Node.HAS_THUMBNAIL_FLAG_MASK, true);
        }
        __writeDirtyNodes();
    }

    /**
     * This method takes a path and deletes the directory the path is pointing to.
     * @param path The path of the directory
     * @param recursive Default value set to false. If set to true, the directory is deleted along with everything that
     *                  is within the directory. If set to false, the directory is only deleted if it is empty. Otherwise,
     *                  an exception is thrown.
     * @throws Exception In case the path does not exist, or the directory is not empty and recursive is not set to true.
     */
    public void removeDirectory(String path, boolean recursive) throws Exception{
        Node node = dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        removeDirectory(node, recursive);
    }

    /**
     * Calls the removeDirectory method with recursive flag set to false
     * @param path Path of the directory
     * @throws Exception In case the Directory is not empty or other IOException occurs.
     */
    public void removeDirectory(String path) throws Exception{
        Node node = dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        removeDirectory(node, false);
    }

    private void removeDirectory(Node node, boolean recursive) throws Exception{
        LinkedList<Node> childNodes;
        if (!recursive){
            gateway.removeDirectory(node);
        } else {
            if (!node.isDirectory()){
                gateway.removeNode(node);
            } else if ((childNodes = node.getChildNodes(gateway.getDirectoryStoreGateway())).isEmpty()){
                gateway.removeDirectory(node);
            } else {
                for (int i = 0; i < childNodes.size(); i++){
                    removeDirectory(childNodes.getFirst(), true);
                }
                gateway.removeDirectory(node);
            }
        }
    }

    /**
     * This method takes a path and deletes the node pointed to by the path.
     * @param path The path of the node.
     * @throws RuntimeException In case the path does not exist.
     */
    public void removeNode(String path) throws Exception{
        Node node = dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        if (node.isDirectory())
            throw new Exception("Node is a directory.");
        gateway.removeNode(node);
    }

    /**
     * This method provides a FileOutputStream to access the required file.
     * @param path The path of the required file
     * @return An InputStream for the requested File.
     */
    public CustomInputStream openFile(String path) throws Exception{
        Node node = dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        return openFile(node);
    }

    public CustomInputStream openFile(Node node) throws Exception{
        if (node.isDirectory())
            throw new Exception("Node is a directory");
        return new CustomInputStream(gateway, gateway.getINode(node));
    }

    public CustomInputStream openThumbnail(String path) throws Exception{
        Node node = dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        return openThumbnail(node);
    }

    public CustomInputStream openThumbnail(Node node) throws Exception{
        if (node.isDirectory())
            throw new Exception("Node is a directory");
        INode iNode = gateway.getINode(node);
        long thumbnailAddress = iNode.getThumbnailStoreAddress();
        if (thumbnailAddress == -1){
            return null;
        }
        return new CustomInputStream(gateway, gateway.getINode(thumbnailAddress), true);
    }

    /**
     * This method provides a Node to access the directory. It does not provide an actual stream of bytes of the data
     * within the Node.
     * @param path The path of the required directory
     * @return A Node object containing the directory
     */
    public LinkedList<Node> openDirectory(String path) throws Exception{
        Node node = dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        return openDirectory(node);
    }
    public LinkedList<Node> openDirectory(Node node) throws Exception{
        if (node.isDirectory()){
            return node.getChildNodes(gateway.getDirectoryStoreGateway());
        } else {
            throw new Exception("Node is not a directory");
        }
    }

    public Node getNode(String path) throws Exception{
        Node node =  dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        gateway.readChildren(node);
        return node;
    }

    /**
     * Takes Node. If its CNR flag is On, read the children if not return as it is.
     * @param node Target Node
     * @return Provided Node with all the children read.
     */
    public Node getNode(Node node) throws Exception{
        gateway.readChildren(node);
        return node;
    }

    /**
     * This method renames a node.
     * @param path The path of the node
     * @param name The desired name for the node
     * @return true if and only if the operation is successful
     */
    public boolean renameNode(String path, String name) throws Exception{
        Node node = dir.getNodeFromPath(path, gateway.getDirectoryStoreGateway());
        return renameNode(node, name);
    }

    public boolean renameNode(Node node, String name) throws Exception{
        if (node == dir.getRoot())
            throw new Exception("Cannot Rename Root");
        Node parentNode = node.parentNode;
        if (dir.__nodeExists(parentNode, name)){
            throw new Exception("Node by the name: "+ name +" already exists");
        }
        node.setName(name);
        dir.getDirtyNodes().add(node);
        __writeDirtyNodes();
        return true;
    }



    public INode getINode(Node node) throws Exception{
        if (node.isDirectory())
            return null;
        return gateway.getINode(node);
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
    // Public Auxiliary Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void printTree() throws Exception{
        __lsChildren(dir.getRoot(), 0);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private Auxiliary Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void __writeDirtyNodes() throws Exception{
        LinkedList<Node> dirtyNodes = dir.getDirtyNodes();
        while (!dirtyNodes.isEmpty())
            gateway.writeNode(dirtyNodes.pop());
    }

    private void __lsChildren(Node node, int level) throws Exception{
        System.out.print("|");
        if (!node.isDirectory()){
            System.out.println("-".repeat(level) + ">" + node.getName());
            return;
        }
        LinkedList<Node> childNodes = node.getChildNodes(gateway.getDirectoryStoreGateway());
        if (childNodes.isEmpty())
            System.out.println("-".repeat(level) + ">" + node.getName());
        else {
            System.out.println("-".repeat(level) + ">" + node.getName());
            for (Node childNode: childNodes){
                __lsChildren(childNode, level + 1);
            }
        }
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
