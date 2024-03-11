package Constants;

public abstract class INODE_FRAME {
    public static final int INODE_FRAME_SIZE = 77;
    public static final int MAGIC_VALUE_1_INDEX = 0;
    public static final int INODE_ADDRESS_INDEX = 4;
    public static final int PARENT_INODE_ADDRESS_INDEX = 12;
    public static final int SIZE_INDEX = 20;
    public static final int FLAGS_INDEX = 28;

    public static final int CREATION_TIME_INDEX = 29;
    public static final int LAST_MODIFIED_TIME_INDEX = 37;
    public static final int MAGIC_VALUE_2_INDEX = 45;
    public static final int EXTENT_STORE_ADDRESS_INDEX = 49;
    public static final int EXTENT_COUNT_INDEX = 57;
    public static final int THUMBNAIL_STORE_ADDRESS_INDEX = 65;
    public static final int MAGIC_VALUE_3_INDEX = 73;
}
