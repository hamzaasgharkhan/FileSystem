package DiskUtility;

import Constants.DIRECTORY_STORE_FRAME;
import Constants.SUPER_BLOCK_BASE_FRAME;
import Constants.VALUES;
import FileSystem.FileSystem;
import FileSystem.INode;
import FileSystem.Node;
import FileSystem.SuperBlock;
import FileSystem.InputFile;

import javax.crypto.SecretKey;
import java.io.*;
import java.security.SecureRandom;
import java.util.LinkedList;

/**
 * Gateway provides the basic initialization operations that the FileSystem requires.
 * It allows the FileSystem to initialize its files for the first time.
 * It allows provides the ability to create other Gateway Objects.
 */
public class Gateway {
    static class NodeEntry{
        Node node;
        INode iNode;
        LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames;
        LinkedList<ExtentStoreGateway.ExtentFrame> thumbnailExtentFrames;
        NodeEntry(Node node, INode iNode, LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames, LinkedList<ExtentStoreGateway.ExtentFrame> thumbnailExtentFrames){
            this.node = node;
            this.iNode = iNode;
            this.extentFrames = extentFrames;
            this.thumbnailExtentFrames = thumbnailExtentFrames;
        }
    }
    private final SuperBlock superBlock;
    private final DirectoryStoreGateway directoryStoreGateway;
    private final INodeStoreGateway iNodeStoreGateway;
    private final ExtentStoreGateway extentStoreGateway;
    private final DataStoreGateway dataStoreGateway;
    private final ThumbnailStoreGateway thumbnailStoreGateway;
    private final BitMapUtility bitMapUtility;
    /**
     * Points to the root directory of the filesystem
     */
    private final File baseFile;
    protected final SecretKey superBlockKey;
    protected SecretKey key;

    public Gateway(File baseFile, SuperBlock superBlock, String password, boolean firstCreation) throws Exception{
        this.superBlock = superBlock;
        this.baseFile = baseFile;
        this.superBlockKey = Crypto.deriveKeyFromPassword(password, new byte[VALUES.SALT_SIZE]);;
        if (firstCreation){
            byte[] salt = new byte[VALUES.SALT_SIZE];
            new SecureRandom().nextBytes(salt);
            this.key = Crypto.deriveKeyFromPassword(password, salt);
            this.superBlock.setSalt(salt);
            initializeFileSystem();
            this.bitMapUtility = new BitMapUtility(baseFile, true);
        } else {
            if (!baseFile.isDirectory()){
                throw new Exception("Gateway Initialization Error: FileSystem Root Directory Does Not Exist or Inaccessible");
            }
            this.bitMapUtility = new BitMapUtility(baseFile);
            this.key = Crypto.deriveKeyFromPassword(password, this.superBlock.getSalt());
        }
        this.directoryStoreGateway = new DirectoryStoreGateway(baseFile, bitMapUtility, key);
        this.iNodeStoreGateway = new INodeStoreGateway(baseFile, bitMapUtility, key);
        this.extentStoreGateway = new ExtentStoreGateway(baseFile, bitMapUtility, key);
        this.dataStoreGateway = new DataStoreGateway(baseFile, bitMapUtility, key);
        this.thumbnailStoreGateway = new ThumbnailStoreGateway(baseFile, bitMapUtility, key);
    }

    public Gateway(File baseFile, SuperBlock superBlock, String password) throws Exception{
        this(baseFile,  superBlock, password, false);
    }
    /**
     * This method initializes the directory for the FileSystem and creates all the necessary files.
     */
    protected void initializeFileSystem() throws Exception{
        try {
            if (baseFile.exists()){
                if (!baseFile.delete()){
                    throw new Exception("FileSystem Creation Failed: Directory with the same name as the FileSystem already" +
                            "exists. Unable to delete the existing directory.");
                }
            }
            if (!baseFile.mkdir()){
                throw new Exception("FileSystem Creation Failed: Unable to create parentDirectory for FileSystem");
            }
            __createSuperBlockFile();
            __createStores();
        } catch (Exception e) {
            throw new IOException("[Gateway] Unable to initialize FileSystem\n"+ e.getMessage());
        }
    }

    /**
     * This method takes the path of the root directory of the FileSystem and returns the FileSystem contained within
     * that directory
     * @param baseFile Path of the root directory of the FileSystem
     * @return FileSystem object containing the FileSystem at the provided path.
     */
    public static FileSystem mountFileSystem(FileSystem fs, File baseFile, String password) throws Exception {
        if (!baseFile.isDirectory())
            throw new IllegalArgumentException("Invalid Path.");
        File superBlockFile;
        try {
            superBlockFile = getFileInBaseDirectory(baseFile, "super-block");
        } catch (Exception e){
            throw new Exception("Unable to get SuperBlock: " + e.getMessage());
        }
        SecretKey key = Crypto.deriveKeyFromPassword(password, new byte[16]);
        SuperBlock superBlock = SuperBlockGateway.getSuperBlock(baseFile, key);
        fs.setGateway(new Gateway(baseFile, superBlock, password));
        fs.setDir(fs.getGateway().directoryStoreGateway.mount());
        return fs;
    }

    private void __createStores() throws Exception{

        for (Store store: Store.values()){
            createFileInBaseDirectory(baseFile, store.fileName);
        }
        try {
            byte[] directoryStoreByteArray = Crypto.encryptBlock(DirectoryStoreGateway.getDefaultBytes(), key, DIRECTORY_STORE_FRAME.SIZE);
            File directoryStoreFile = getFileInBaseDirectory(baseFile, Store.DirectoryStore.fileName);
            FileOutputStream fout = new FileOutputStream(directoryStoreFile);
            fout.write(directoryStoreByteArray);
            fout.close();
        } catch (IOException e){
            throw new Exception("Error Initializing File System Store: directory-store");
        } catch (Exception e){
            throw new Exception("Error Initializing File System Store: directory-store. Encryption Error");
        }
    }

    private void __createSuperBlockFile() throws Exception{
        File superBlockFile = createFileInBaseDirectory(baseFile,"super-block");
        try {
            byte[] superBlockArray = Crypto.encryptBlock(SuperBlockGateway.getBytes(superBlock), superBlockKey, SUPER_BLOCK_BASE_FRAME.SIZE);
            FileOutputStream fout = new FileOutputStream(superBlockFile);
            fout.write(superBlockArray);
            fout.close();
        } catch (IOException e){
            throw new IOException("[Gateway] __createSuperBlockFile failed.\n" + e.getMessage());
        } catch (Exception e){
            throw new Exception("[Gateway] __createSuperBlockFile failed.\n" + e.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Adding nodes to directory store
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public long writeNode(Node node) throws Exception{
        if (node.getIndex() == -1L){
            return directoryStoreGateway.addNode(node);
        } else {
            return directoryStoreGateway.updateNode(node);
        }
    }

    public DirectoryStoreGateway getDirectoryStoreGateway() {
        return directoryStoreGateway;
    }
    public DataStoreGateway getDataStoreGateway() {return dataStoreGateway;}
    public ThumbnailStoreGateway getThumbnailStoreGateway() {return thumbnailStoreGateway;}
    public INodeStoreGateway getiNodeStoreGateway() {return iNodeStoreGateway;}

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Adding actual data files.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public INode addFile(InputFile file) throws Exception{
        long[] extentStoreDetails = extentStoreGateway.addExtentEntry(dataStoreGateway.addNode(file));
        long[] thumbnailExtentStoreDetails = null;
        long thumbnailStoreAddress = -1;
        if (file.thumbnailInputStream != null){
            thumbnailExtentStoreDetails = extentStoreGateway.addExtentEntry(thumbnailStoreGateway.addNode(file));
            thumbnailStoreAddress = iNodeStoreGateway.addThumbnailNode(file, thumbnailExtentStoreDetails).getiNodeAddress();
        }
        file.close();
        return iNodeStoreGateway.addNode(file, extentStoreDetails, thumbnailStoreAddress);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Removing actual data files
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void removeNode(Node node) throws Exception{
        NodeEntry nodeEntry = __getNodeDetails(node);
        dataStoreGateway.removeNode(nodeEntry.extentFrames);
        thumbnailStoreGateway.removeNode(nodeEntry.thumbnailExtentFrames);
        // FIX THIS
        extentStoreGateway.removeExtentEntry(nodeEntry.extentFrames);
        extentStoreGateway.removeExtentEntry(nodeEntry.thumbnailExtentFrames);
        iNodeStoreGateway.removeINode(nodeEntry.iNode.getiNodeAddress());
        iNodeStoreGateway.removeINode(nodeEntry.iNode.getThumbnailStoreAddress());
        directoryStoreGateway.removeNode(node);
    }

    public void removeDirectory(Node node) throws Exception{
        if (!node.checkFlag(Node.DIRECTORY_FLAG_MASK))
            throw new Exception("Node is not a directory.");
        directoryStoreGateway.removeNode(node);
    }

    public void readChildren(Node node) throws Exception {
        directoryStoreGateway.readChildren(node);
    }

    public INode getINode(Node node) throws Exception{
        return iNodeStoreGateway.getINode(node.getiNodeAddress());
    }

    public INode getINode(long iNodeAddress) throws Exception{
        return iNodeStoreGateway.getINode(iNodeAddress);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  HELPER METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Takes a node and returns all the information related to that Node.
     * @param node The target Node
     * @return NodeEntry object containing the details regarding the node.
     */
    NodeEntry __getNodeDetails(Node node) throws Exception{
        INode iNode = iNodeStoreGateway.getINode(node.getiNodeAddress());
        LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames = extentStoreGateway.getExtentFrames(iNode.getExtentStoreAddress(), iNode.getExtentCount());
        LinkedList<ExtentStoreGateway.ExtentFrame> thumbnailFrames = null;
        long thumbnailAddress = iNode.getThumbnailStoreAddress();
        if (thumbnailAddress != -1){
            INode thumbnailINode = iNodeStoreGateway.getINode(thumbnailAddress);
            thumbnailFrames = extentStoreGateway.getExtentFrames(thumbnailINode.getExtentStoreAddress(), thumbnailINode.getExtentCount());
        }
        return new NodeEntry(node, iNode, extentFrames, thumbnailFrames);
    }

    NodeEntry __getINodeDetails(INode iNode) throws Exception{
        LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames = extentStoreGateway.getExtentFrames(iNode.getExtentStoreAddress(), iNode.getExtentCount());
        LinkedList<ExtentStoreGateway.ExtentFrame> thumbnailFrames = null;
        long thumbnailAddress = iNode.getThumbnailStoreAddress();
        if (thumbnailAddress != -1){
            INode thumbnailINode = iNodeStoreGateway.getINode(thumbnailAddress);
            thumbnailFrames = extentStoreGateway.getExtentFrames(thumbnailINode.getExtentStoreAddress(), thumbnailINode.getExtentCount());
        }
        return new NodeEntry(null, iNode, extentFrames, thumbnailFrames);
    }

    /**
     * This method creates an empty file of the given name within the FileSystem baseDirectory. If a file by the provided name
     * exists, the method attempts to delete the file.
     * @param name Name of the new file.
     * @return File object if and only if the File has been created
     * @throws Exception In case the file was not created.
     */
    protected static File createFileInBaseDirectory(File baseDirectory, String name) throws Exception{
        File file = new File(baseDirectory, name);
        if (file.exists()){
            if (!file.delete()){
                throw new Exception("File Creation Failed. File already exists in the base directory. Unable to delete" +
                        "existing file: " + file);
            }
        }
        if (!file.createNewFile()){
            throw new Exception("File Creation Failed: " + file);
        }
        return file;
    }

    protected static File getFileInBaseDirectory(File baseDirectory, String name) throws Exception{
        File file = new File(baseDirectory, name);
        if (!file.isFile()){
            throw new Exception("Unable to Get File. The provided name does need have a file associated with it in " +
                    "the base directory or is inaccessible (potentially due to lack of permissions)");
        }
        return file;
    }

    protected static byte[] readAllBytes(File file) throws IOException{
        try (InputStream inputStream = new FileInputStream(file);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()){

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1){
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    protected static byte[] readNBytes(File file, byte[] buffer, int bytesToRead) throws IOException{
        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()){

            int bytesRead;

            while (((bytesRead = inputStream.read(buffer)) != bytesToRead) && (bytesRead != -1)){
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            if (bytesRead == -1){
                throw new EOFException("End of File Reached");
            }
            return byteArrayOutputStream.toByteArray();
        }
    }
}
