package DiskUtility;

import Constants.DIRECTORY_STORE_FRAME;
import Constants.SUPER_BLOCK_BASE_FRAME;
import Constants.VALUES;
import FileSystem.FileSystem;
import FileSystem.SuperBlock;
import FileSystem.Node;
import FileSystem.INode;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.LinkedList;

/**
 * Gateway provides the basic initialization operations that the FileSystem requires.
 * It allows the FileSystem to initialize its files for the first time.
 * It allows provides the ability to create other Gateway Objects.
 */
public class Gateway {
    public static class NodeEntry{
        Node node;
        INode iNode;
        LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames;
        public NodeEntry(Node node, INode iNode, LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames){
            this.node = node;
            this.iNode = iNode;
            this.extentFrames = extentFrames;
        }
    }
    private final SuperBlock superBlock;
    private final DirectoryStoreGateway directoryStoreGateway;
    private final INodeStoreGateway iNodeStoreGateway;
    private final ExtentStoreGateway extentStoreGateway;
    private final DataStoreGateway dataStoreGateway;
    private final ThumbnailStoreGateway thumbnailStoreGateway;
    private final BitMapUtility bitMapUtility;
    private final Path basePath;
    protected final SecretKey superBlockKey;
    protected SecretKey key;

    public Gateway(Path basePath, SuperBlock superBlock, String password, boolean firstCreation) throws Exception{
        this.superBlock = superBlock;
        this.basePath = basePath;
        this.superBlockKey = Crypto.deriveKeyFromPassword(password, new byte[VALUES.SALT_SIZE]);;
        if (firstCreation){
            byte[] salt = new byte[VALUES.SALT_SIZE];
            new SecureRandom().nextBytes(salt);
            this.key = Crypto.deriveKeyFromPassword(password, salt);
            this.superBlock.setSalt(salt);
            initializeFileSystem();
            this.bitMapUtility = new BitMapUtility(basePath, true);
        } else {
            this.bitMapUtility = new BitMapUtility(basePath);
            this.key = Crypto.deriveKeyFromPassword(password, this.superBlock.getSalt());
        }
        this.directoryStoreGateway = new DirectoryStoreGateway(basePath, bitMapUtility, key);
        this.iNodeStoreGateway = new INodeStoreGateway(basePath, bitMapUtility, key);
        this.extentStoreGateway = new ExtentStoreGateway(basePath, bitMapUtility, key);
        this.dataStoreGateway = new DataStoreGateway(basePath, bitMapUtility, key);
        this.thumbnailStoreGateway = new ThumbnailStoreGateway(basePath, bitMapUtility, key);
    }

    public Gateway(Path path, SuperBlock superBlock, String password) throws Exception{
        this(path,  superBlock, password, false);
    }
    /**
     * This method initializes the directory for the FileSystem and creates all the necessary files.
     */
    protected void initializeFileSystem() throws Exception{
        try {
            Files.createDirectory(basePath);
            __createSuperBlockFile();
            __createStores();
        } catch (Exception e) {
            throw new IOException("[Gateway] Unable to initialize FileSystem\n"+ e.getMessage());
        }
    }

    /**
     * This method takes the path of the root directory of the FileSystem and returns the FileSystem contained within
     * that directory
     * @param path Path of the root directory of the FileSystem
     * @return FileSystem object containing the FileSystem at the provided path.
     */
    public static FileSystem mountFileSystem(FileSystem fs, Path path, String password) throws Exception {
        File file = path.toFile();
        if (!file.exists() || !file.isDirectory())
            throw new IllegalArgumentException("Invalid Path.");
        File superBlockFile = new File(path + "/super-block");
        if (!superBlockFile.exists())
            throw new Exception("SuperBlock File Does Not Exist.");
        SecretKey key = Crypto.deriveKeyFromPassword(password, new byte[16]);
        SuperBlock superBlock = SuperBlockGateway.getSuperBlock(path, key);
        fs.setSuperBlock(superBlock);
        fs.setGateway(new Gateway(path, superBlock, password));
        fs.setDir(fs.getGateway().directoryStoreGateway.mount());
        return fs;
    }

    private void __createStores() throws Exception{
        // Directory store is not added to this list as it will not be empty.
        String[] storeNames = {
                "extent-store",
                "inode-store",
                "data-store",
                "thumbnail-store"
        };
        for (String storeName: storeNames){
            try {
                Files.createFile(basePath.resolve(storeName));
            } catch (IOException e){
                throw new Exception("Error Initializing File System Store: " + storeName);
            }
        }
        try {
            byte[] directoryStoreByteArray = Crypto.encryptBlock(DirectoryStoreGateway.getDefaultBytes(), key, DIRECTORY_STORE_FRAME.SIZE);
            Files.write(basePath.resolve("directory-store"), directoryStoreByteArray);
        } catch (IOException e){
            throw new Exception("Error Initializing File System Store: directory-store");
        } catch (Exception e){
            throw new Exception("Error Initializing File System Store: directory-store. Encryption Error");
        }
    }

    private void __createSuperBlockFile() throws Exception{
        Path path = Paths.get(superBlock.getFileSystemName() + "/super-block");
        try {
            byte[] superBlockArray = Crypto.encryptBlock(SuperBlockGateway.getBytes(superBlock), superBlockKey, SUPER_BLOCK_BASE_FRAME.SIZE);
            Files.write(path, superBlockArray);
        } catch (IOException e){
            throw new IOException("[Gateway] __createSuperBlockFile failed.\n" + e.getMessage());
        } catch (Exception e){
            throw new Exception("[Gateway] __createSuperBlockFile failed.\n" + e.getMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Adding nodes to directory store
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public long addNode(Node node) throws Exception{
        return directoryStoreGateway.addNode(node);
    }

    public DirectoryStoreGateway getDirectoryStoreGateway() {
        return directoryStoreGateway;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Adding actual data files.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public INode addFile(Path path) throws Exception{
        File file = path.toFile();
        long[] extentStoreDetails = extentStoreGateway.addExtentEntry(dataStoreGateway.addNode(file));
        long thumbnailStoreAddress = thumbnailStoreGateway.addNode(path);
        return iNodeStoreGateway.addNode(path, extentStoreDetails, thumbnailStoreAddress);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  Removing actual data files
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void removeNode(Node node) throws Exception{
        NodeEntry nodeEntry = __getNodeDetails(node);
        dataStoreGateway.removeNode(nodeEntry.extentFrames);
        thumbnailStoreGateway.removeNode(nodeEntry.iNode.getThumbnailStoreAddress());
        extentStoreGateway.removeExtentEntry(nodeEntry.extentFrames);
        iNodeStoreGateway.removeINode(nodeEntry.iNode.getiNodeAddress());
        directoryStoreGateway.removeNode(node);
    }

    public void removeDirectory(Node node) throws Exception{
        if (!node.checkFlag(Node.DIRECTORY_FLAG_MASK))
            throw new Exception("Node is not a directory.");
        directoryStoreGateway.removeNode(node);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  HELPER METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Takes a node and returns all the information related to that Node.
     * @param node The target Node
     * @return NodeEntry object containing the details regarding the node.
     */
    private NodeEntry __getNodeDetails(Node node) throws Exception{
        INode iNode = iNodeStoreGateway.getINode(node.getiNodeAddress());
        LinkedList<ExtentStoreGateway.ExtentFrame> extentFrames = extentStoreGateway.getExtentFrames(iNode.getExtentStoreAddress(), iNode.getExtentCount());
        return new NodeEntry(node, iNode, extentFrames);
    }
}
