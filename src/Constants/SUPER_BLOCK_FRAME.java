package Constants;

/**
 * This enumeration contains the starting index for the given values in the SUPERBLOCK file.
 * Since the location for DATA_STORES and MAGIC_VALUE_4 is calculated at runtime, their values have been assigned as -1.
 */
public abstract class SUPER_BLOCK_FRAME {
    /**
     * Size of a SuperBlock excluding the arrays (thumbnailStores and dataStores)
     */
    public final static int SUPER_BLOCK_SIZE = 317;
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
    public static final int MAGIC_VALUE_3_INDEX = 313;
}
