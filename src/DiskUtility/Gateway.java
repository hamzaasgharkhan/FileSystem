package DiskUtility;

import Constants.PRESET_FILE_NAMES;
import FileSystem.FileSystem;
import FileSystem.SuperBlock;
import FileSystem.Node;
import FileSystem.INode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Gateway provides the basic initialization operations that the FileSystem requires.
 * It allows the FileSystem to initialize its files for the first time.
 * It allows provides the ability to create other Gateway Objects.
 */
public class Gateway {
    private final SuperBlock superBlock;
    private final DirectoryStoreGateway directoryStoreGateway;
    private final INodeStoreGateway iNodeStoreGateway;
    private final ExtentStoreGateway extentStoreGateway;
    private final DataStoreGateway dataStoreGateway;
    private final ThumbnailStoreGateway thumbnailStoreGateway;
    private final BitMapUtility bitMapUtility;
    private final Path basePath;

    public Gateway(Path basePath, SuperBlock superBlock, boolean firstCreation) throws Exception{
        this.superBlock = superBlock;
        this.basePath = basePath;
        if (firstCreation){
            initializeFileSystem();
            this.bitMapUtility = new BitMapUtility(basePath, true);
        } else {
            this.bitMapUtility = new BitMapUtility(basePath);
        }
        this.directoryStoreGateway = new DirectoryStoreGateway(basePath, bitMapUtility);
        this.iNodeStoreGateway = new INodeStoreGateway(basePath, bitMapUtility);
        this.extentStoreGateway = new ExtentStoreGateway(basePath, bitMapUtility);
        this.dataStoreGateway = new DataStoreGateway(basePath, bitMapUtility);
        this.thumbnailStoreGateway = new ThumbnailStoreGateway(basePath, bitMapUtility);
    }

    public Gateway(Path path, SuperBlock superBlock) throws Exception{
        this(path,  superBlock, false);
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
    public static FileSystem mountFileSystem(FileSystem fs, Path path) throws Exception {
        File file = path.toFile();
        if (!file.exists() || !file.isDirectory())
            throw new IllegalArgumentException("Invalid Path.");
        File superBlockFile = new File(path + "/super-block");
        if (!superBlockFile.exists())
            throw new Exception("SuperBlock File Does Not Exist.");
        SuperBlock superBlock = SuperBlockGateway.getSuperBlock(path);
        fs.setSuperBlock(superBlock);
        fs.setGateway(new Gateway(path, superBlock));
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
            Files.write(basePath.resolve("directory-store"), DirectoryStoreGateway.getDefaultBytes());
        } catch (IOException e){
            throw new Exception("Error Initializing File System Store: directory-store");
        }
    }

    private void __createSuperBlockFile() throws IOException{
        Path path = Paths.get(superBlock.getFileSystemName() + "/super-block");
        byte[] superBlockArray = SuperBlockGateway.getBytes(superBlock);
        try {
            Files.write(path, superBlockArray);
        } catch (IOException e){
            throw new IOException("[Gateway] __createSuperBlockFile failed.\n" + e.getMessage());
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
}
