////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Methods:
//      createSuperBlockGateway()
//      getSuperBlockGateway()
//      getSuperBlock()
//      writeSuperBlock()
//
//      flags -- byte to store flags.
//      X X X X X C E F
//      F - First Access
//      E - The SuperBlock file is empty.
//      C - Correct
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
package DiskUtility;

import Constants.SUPER_BLOCK_BASE_FRAME;
import Constants.VALUES;
import FileSystem.SuperBlock;
import Utilities.BinaryUtilities;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * This class serves as a Gateway between the SuperBlock class and the actual files on disk that contain the SuperBlock.
 */
public abstract class SuperBlockGateway{
    protected static byte[] getBytes(SuperBlock superBlock){
        final byte[] byteArray = new byte[SUPER_BLOCK_BASE_FRAME.SIZE];
        // SET MAGIC VALUES
        System.arraycopy(VALUES.MAGIC_VALUE_BYTES, 0, byteArray, SUPER_BLOCK_BASE_FRAME.MAGIC_VALUE_1_INDEX, 4);
        System.arraycopy(VALUES.MAGIC_VALUE_BYTES, 0, byteArray, SUPER_BLOCK_BASE_FRAME.MAGIC_VALUE_2_INDEX, 4);
        System.arraycopy(VALUES.MAGIC_VALUE_BYTES, 0, byteArray, SUPER_BLOCK_BASE_FRAME.MAGIC_VALUE_3_INDEX, 4);
        // FILE_SYSTEM_NAME
        System.arraycopy(superBlock.getFieldBytes("FILE_SYSTEM_NAME"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.FILE_SYSTEM_NAME_INDEX,
                256);
        // DIRECTORY_STORES
        System.arraycopy(superBlock.getFieldBytes("DIRECTORY_STORES"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.DIRECTORY_STORES_INDEX,
                8);
        // INODE_STORES
        System.arraycopy(superBlock.getFieldBytes("INODE_STORES"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.INODE_STORES_INDEX,
                8);
        // EXTENT_STORES
        System.arraycopy(superBlock.getFieldBytes("EXTENT_STORES"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.EXTENT_STORES_INDEX,
                8);
        // FLAGS
        byteArray[SUPER_BLOCK_BASE_FRAME.FLAGS_INDEX] = superBlock.getFlags();
        // THUMBNAIL_STORES
        System.arraycopy(superBlock.getFieldBytes("THUMBNAIL_STORES"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.THUMBNAIL_STORES_INDEX,
                8);
        // DATA_STORES
        System.arraycopy(superBlock.getFieldBytes("DATA_STORES"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.DATA_STORES_INDEX,
                8);
        // ATTRIBUTE_STORES
        System.arraycopy(superBlock.getFieldBytes("ATTRIBUTE_STORES"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.ATTRIBUTE_STORES_INDEX,
                8);
        // ATTRIBUTE_STORES
        System.arraycopy(superBlock.getFieldBytes("SALT"),
                0,
                byteArray,
                SUPER_BLOCK_BASE_FRAME.SALT_VALUE_INDEX,
                16);
        return byteArray;
    }
    /**
     * This method extracts the SuperBlock object from the file on disk.
     * @param path The path to the FileSystem.
     * @return SuperBlock Object stored in the SuperBlock file
     */
    protected static SuperBlock getSuperBlock(Path path, SecretKey key) throws Exception{
        File file = path.resolve("super-block").toFile();
        if (!file.exists()){
            throw new Exception("SuperBlock File Does Not Exist.");
        }
        byte[] byteArray;
        FileInputStream fin;
        try {
            fin = new FileInputStream(file);
            byteArray = fin.readAllBytes();
            fin.close();
        } catch (FileNotFoundException e){
            throw new Exception("Could Not Retrieve SuperBlock. SuperBlock File Not Found.\n" + e.getMessage());
        }
        byteArray = Crypto.decryptBlock(byteArray, key, SUPER_BLOCK_BASE_FRAME.SIZE);
        String fileSystemName = BinaryUtilities.convertBytesToUTF8String(byteArray, SUPER_BLOCK_BASE_FRAME.FILE_SYSTEM_NAME_INDEX, 256).trim();
        long directoryStores = BinaryUtilities.convertBytesToLong(byteArray, SUPER_BLOCK_BASE_FRAME.DIRECTORY_STORES_INDEX);
        long iNodeStores = BinaryUtilities.convertBytesToLong(byteArray, SUPER_BLOCK_BASE_FRAME.INODE_STORES_INDEX);
        long extentStores = BinaryUtilities.convertBytesToLong(byteArray, SUPER_BLOCK_BASE_FRAME.EXTENT_STORES_INDEX);
        byte flags = byteArray[SUPER_BLOCK_BASE_FRAME.FLAGS_INDEX];
        long thumbnailStores = BinaryUtilities.convertBytesToLong(byteArray, SUPER_BLOCK_BASE_FRAME.THUMBNAIL_STORES_INDEX);
        long dataStores = BinaryUtilities.convertBytesToLong(byteArray, SUPER_BLOCK_BASE_FRAME.DATA_STORES_INDEX);
        long attributeStores = BinaryUtilities.convertBytesToLong(byteArray, SUPER_BLOCK_BASE_FRAME.ATTRIBUTE_STORES_INDEX);
        byte[] salt = Arrays.copyOfRange(byteArray, SUPER_BLOCK_BASE_FRAME.SALT_VALUE_INDEX, SUPER_BLOCK_BASE_FRAME.SALT_VALUE_INDEX + VALUES.SALT_SIZE);
        return new SuperBlock(fileSystemName, directoryStores, iNodeStores, extentStores, flags, thumbnailStores,
                dataStores, attributeStores, salt);
    }
}
