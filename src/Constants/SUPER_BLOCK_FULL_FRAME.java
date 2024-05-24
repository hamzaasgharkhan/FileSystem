package Constants;

/**
 *  IV Value                    -       12 bytes                                || Starting Index: 0
 *  *     Magic Value                 -       4 bytes                                 || Starting Index: 12
 *  *     Flags                       -       1 byte                                  || Starting Index: 16
 *  *     FileSystem Name (UTF-8)     -       256 bytes                               || Starting Index: 17
 *  *     Magic Value                 -       4 bytes                                 || Starting Index: 273
 *  *     Total DirectoryStores       -       8 bytes                                 || Starting Index: 277
 *  *     Total DataStores            -       8 bytes                                 || Starting Index: 285
 *  *     Total ThumbnailStores       -       8 bytes                                 || Starting Index: 293
 *  *     Total INodeStores           -       8 bytes                                 || Starting Index: 301
 *  *     Total ExtentStores          -       8 bytes                                 || Starting Index: 309
 *  *     Total AttributeStores       -       8 bytes                                 || Starting Index: 317
 *  *     SALT Value                  -       16 bytes                                || Starting Index: 325
 *  *     Magic Value                 -       4 bytes                                 || Starting Index: 341
 *  *     TAG                         -       16 bytes                                || Starting Index: 345
 */
public abstract class SUPER_BLOCK_FULL_FRAME {
        public static final int SIZE = 361;
    public static final int IV_INDEX = 0;
    public static final int MAGIC_VALUE_1_INDEX = 12;
    public static final int FLAGS_INDEX = 16;
    public static final int FILE_SYSTEM_NAME_INDEX = 17;
    public static final int MAGIC_VALUE_2_INDEX = 273;
    public static final int DIRECTORY_STORES_INDEX = 277;
    public static final int DATA_STORES_INDEX = 285;
    public static final int THUMBNAIL_STORES_INDEX = 293;
    public static final int INODE_STORES_INDEX = 301;
    public static final int EXTENT_STORES_INDEX = 309;
    public static final int ATTRIBUTE_STORES_INDEX = 317;
    public static final int SALT_VALUE_INDEX = 325;
    public static final int MAGIC_VALUE_3_INDEX = 341;
    public static final int TAG_INDEX = 345;
}
