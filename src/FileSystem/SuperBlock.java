package FileSystem;

import Constants.FLAGS;
import Constants.VALUES;
import Utilities.BinaryUtilities;
import Utilities.GeneralUtilities;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SuperBlock {
    private String fileSystemName;
    private long directoryStores;
    private long iNodeStores;
    private long extentStores;
    private byte flags;
    private long thumbnailStores;
    private long dataStores;
    private long attributeStores;

    /**
     * Constructor for SuperBlock
     * @param fileSystemName Name of the FileSystem (UTF-8). Cannot exceed 256 bytes.
     * @param directoryStores Number of DirectoryStores
     * @param iNodeStores Number of iNodeStores
     * @param extentStores Number of extentStores
     * @param flags flags
     * @param thumbnailStores Number of thumbnailStores
     * @param dataStores Number of dataStores
     * @param attributeStores attributeStores
     * @throws IllegalArgumentException If the fileSystemName is blank or exceeds 256 bytes
     */
    public SuperBlock(String fileSystemName,
                      long directoryStores,
                      long iNodeStores,
                      long extentStores,
                      byte flags,
                      long thumbnailStores,
                      long dataStores,
                      long attributeStores) {
        if (fileSystemName.isEmpty())
            throw new IllegalArgumentException("FileSystem Name Cannot Be Blank");
        if (fileSystemName.getBytes().length > 256)
            throw new IllegalArgumentException("FileSystem Name Cannot Exceed 256 bytes");
        this.fileSystemName = fileSystemName;
        this.directoryStores = directoryStores;
        this.iNodeStores = iNodeStores;
        this.extentStores = extentStores;
        this.flags = flags;
        this.thumbnailStores = thumbnailStores;
        this.dataStores = dataStores;
        this.attributeStores = attributeStores;
    }

    public SuperBlock(String fileSystemName){
        this(fileSystemName, 1, 1, 1, FLAGS.DEFAULT_SUPER_BLOCK, 1, 1, 1);
    }

    /**
     * This method returns the byte Array representation of any field of the SuperBlock
     * @param field The name of the field. Can be one of the following:
     *              FILE_SYSTEM_NAME
     *              DIRECTORY_STORES
     *              INODE_STORES
     *              EXTENT_STORES
     *              FLAGS
     *              THUMBNAIL_STORES
     *              DATA_STORES
     *              ATTRIBUTE_STORES
     * @return A byte array containing the desired field
     */
    public byte[] getFieldBytes(String field){
        return switch (field) {
            case "FILE_SYSTEM_NAME" -> GeneralUtilities.getFixedSizeUTF8StringBytes(fileSystemName, 256);
            case "DIRECTORY_STORES" -> BinaryUtilities.convertLongToBytes(directoryStores);
            case "INODE_STORES" -> BinaryUtilities.convertLongToBytes(iNodeStores);
            case "EXTENT_STORES" -> BinaryUtilities.convertLongToBytes(extentStores);
            case "THUMBNAIL_STORES" -> BinaryUtilities.convertLongToBytes(thumbnailStores);
            case "DATA_STORES" -> BinaryUtilities.convertLongToBytes(dataStores);
            case "ATTRIBUTE_STORES" -> BinaryUtilities.convertLongToBytes(attributeStores);
            case "FLAGS" -> new byte[]{flags};
            default -> throw new IllegalArgumentException("No such field exists");
        };
    }

    public String getFileSystemName() {
        return fileSystemName;
    }

    public void setFileSystemName(String fileSystemName) {
        this.fileSystemName = fileSystemName;
    }

    public long getDirectoryStores() {
        return directoryStores;
    }

    public void setDirectoryStores(long directoryStores) {
        this.directoryStores = directoryStores;
    }

    public long getiNodeStores() {
        return iNodeStores;
    }

    public void setiNodeStores(long iNodeStores) {
        this.iNodeStores = iNodeStores;
    }

    public long getExtentStores() {
        return extentStores;
    }

    public void setExtentStores(long extentStores) {
        this.extentStores = extentStores;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public long getThumbnailStores() {
        return thumbnailStores;
    }

    public void setThumbnailStores(long thumbnailStores) {
        this.thumbnailStores = thumbnailStores;
    }

    public long getDataStores() {
        return dataStores;
    }

    public void setDataStores(long dataStores) {
        this.dataStores = dataStores;
    }

    public long getAttributeStores() {
        return attributeStores;
    }

    public void setAttributeStores(long attributeStores) {
        this.attributeStores = attributeStores;
    }
}
