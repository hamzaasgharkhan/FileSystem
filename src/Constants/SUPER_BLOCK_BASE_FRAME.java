package Constants;

/**
 */
public abstract class SUPER_BLOCK_BASE_FRAME {
    /**
     * Size of a SuperBlock excluding the arrays (thumbnailStores and dataStores)
     */
    public static final int SIZE = 333; // Size excluding IV and TAG.
    public static final int MAGIC_VALUE_1_INDEX = 0;
    public static final int FLAGS_INDEX = 4;
    public static final int FILE_SYSTEM_NAME_INDEX = 5;
    public static final int MAGIC_VALUE_2_INDEX = 261;
    public static final int DIRECTORY_STORES_INDEX = 265;
    public static final int DATA_STORES_INDEX = 273;
    public static final int THUMBNAIL_STORES_INDEX = 281;
    public static final int INODE_STORES_INDEX = 289;
    public static final int EXTENT_STORES_INDEX = 297;
    public static final int ATTRIBUTE_STORES_INDEX = 305;
    public static final int SALT_VALUE_INDEX = 313;
    public static final int MAGIC_VALUE_3_INDEX = 329;
}
