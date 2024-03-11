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

/**
 * Gateway provides the basic initialization operations that the FileSystem requires.
 * It allows the FileSystem to initialize its files for the first time.
 * It allows provides the ability to create other Gateway Objects.
 */
public class Gateway {
    private final SuperBlock superBlock;
    private DirectoryStoreGateway directoryStoreGateway;
    private INodeStoreGateway iNodeStoreGateway;
    private BitMapUtility bitMapUtility;

    public Gateway(SuperBlock superBlock, boolean firstCreation) throws Exception{
        this.superBlock = superBlock;
        if (!firstCreation){
            this.bitMapUtility = new BitMapUtility(superBlock.getFileSystemName());
            this.directoryStoreGateway = new DirectoryStoreGateway(superBlock.getFileSystemName() + "/directory-store", bitMapUtility);
            this.iNodeStoreGateway = new INodeStoreGateway(superBlock.getFileSystemName() + "/inode-store", bitMapUtility);
        } else {
            initializeFileSystem();
        }
    }

    public Gateway(SuperBlock superBlock) throws Exception{
        this(superBlock, false);
    }

    /**
     * This method initializes the directory for the FileSystem and creates all the necessary files.
     */
    private void initializeFileSystem() throws Exception{
        Path basePath = Paths.get(superBlock.getFileSystemName());
        try {
            Files.createDirectory(basePath);
            __createSuperBlockFile();
            __createDirectoryStoreFile();
            __createExtentStoreFile();
            __createINodeStoreFile();
            __createThumbnailStoreFile();
            __createDataStoreFile();
            __createAttributeStoreFile();
            __createBitMapFiles();
            bitMapUtility = new BitMapUtility(superBlock.getFileSystemName());
            directoryStoreGateway = new DirectoryStoreGateway(superBlock.getFileSystemName() + "/directory-store", bitMapUtility);
            iNodeStoreGateway = new INodeStoreGateway(superBlock.getFileSystemName() + "/inode-store", bitMapUtility);
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
    public static FileSystem mountFileSystem(FileSystem fs, String path) throws Exception {
        File file = new File(path);
        if (!file.exists() || !file.isDirectory())
            throw new IllegalArgumentException("Invalid Path.");
        File superBlockFile = new File(path + "/super-block");
        if (!superBlockFile.exists())
            throw new Exception("SuperBlock File Does Not Exist.");
        SuperBlock superBlock = SuperBlockGateway.getSuperBlock(path);
        fs.setSuperBlock(superBlock);
        fs.setGateway(new Gateway(superBlock));
        fs.setDir(fs.getGateway().directoryStoreGateway.mount());
        return fs;
    }

    private void __createBitMapFiles() throws IOException {
        String prefix = superBlock.getFileSystemName() + "/";
        String[] paths = {
                "extent-store.bitmap",
                "inode-store.bitmap",
        };
        try {
            for (String name: paths) {
                Path path = Paths.get(prefix + name);
                Files.createFile(path);
            }
            // Set Directory Bitmap to have 1 slot taken to represent root.
            Path path = Paths.get(prefix + "directory-store.bitmap");
            byte[] blockArray = new byte[16];
            blockArray[0] = (byte) 0b10000000;
            Files.write(path, blockArray);
            // Set AttributeStore Bitmap to empty
            path = Paths.get(prefix + "attribute-store.bitmap");
            blockArray[0] = 0x00;
            Files.write(path, blockArray);
            // Set DataStore Bitmap to empty
            path = Paths.get(prefix + "data-store.bitmap");
            blockArray = new byte[]{(byte)0b10001001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001,
                    (byte)0b10011001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001,
                    (byte)0b10011001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001,
                    (byte)0b10011001, (byte)0b10011001, (byte)0b10011001, (byte)0b10011001};
            Files.write(path, blockArray);
            // Set ThumbnailStore Bitmap to empty
            path = Paths.get(prefix + "thumbnail-store.bitmap");
            Files.write(path, blockArray);
        } catch (IOException e){
            throw new IOException("[Gateway] __createBitMapFiles Failed.\n" + e.getMessage());
        }
    }

    private void __createThumbnailStoreFile() throws IOException {
        Path path = Paths.get(superBlock.getFileSystemName() + "/thumbnail-store");
        byte[] blockArray = ThumbnailStoreGateway.getDefaultBytes();
        try {
            Files.write(path, blockArray);
        } catch (IOException e){
            throw new IOException("[Gateway] __createThumbnailStoreFileFailed failed.\n" + e.getMessage());
        }
    }

    private void __createINodeStoreFile() throws IOException{
        Path path = Paths.get(superBlock.getFileSystemName() + "/inode-store");
        try {
            Files.createFile(path);
        } catch (IOException e){
            throw new IOException("[Gateway] __createINodeStoreFileFailed failed.\n" + e.getMessage());
        }
    }

    private void __createAttributeStoreFile() throws IOException{
        Path path = Paths.get(superBlock.getFileSystemName() + "/attribute-store");
        try {
            Files.createFile(path);
        } catch (IOException e){
            throw new IOException("[Gateway] __createAttributeStoreFileFailed failed.\n" + e.getMessage());
        }
    }

    private void __createExtentStoreFile() throws IOException {
        Path path = Paths.get(superBlock.getFileSystemName() + "/extent-store");
        try {
            Files.createFile(path);
        } catch (IOException e){
            throw new IOException("[Gateway] __createExtentStoreFileFailed failed.\n" + e.getMessage());
        }
    }

    private void __createDirectoryStoreFile() throws IOException{
        Path path = Paths.get(superBlock.getFileSystemName() + "/directory-store");
        byte[] blockArray = DirectoryStoreGateway.getDefaultBytes();
        try {
            Files.write(path, blockArray);
        } catch (IOException e){
            throw new IOException("[Gateway] __createDirectoryStoreFileFailed failed.\n" + e.getMessage());
        }
    }

    private void __createDataStoreFile() throws IOException{
        Path path = Paths.get(superBlock.getFileSystemName() + "/data-store");
        byte[] blockArray = DataStoreGateway.getDefaultBytes();
        try {
            Files.write(path, blockArray);
        } catch (IOException e){
            throw new IOException("[Gateway] __createDataStoreFileFailed failed.\n" + e.getMessage());
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
    public INode addFile(File file) throws Exception{
        return iNodeStoreGateway.addNode(file);
    }
}
