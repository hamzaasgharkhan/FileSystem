package Constants;
public abstract class DIRECTORY_STORE_FRAME {
    public static final int SIZE = 325;
    public static final int FULL_SIZE = SIZE + VALUES.IV_SIZE + VALUES.TAG_SIZE;
    public static final int MAGIC_VALUE_1_INDEX = 0;
    public static final int DIRECTORY_STORE_INDEX_INDEX = 4;
    public static final int PARENT_INDEX = 12;
    public static final int PREVIOUS_SIBLING_INDEX = 20;
    public static final int NEXT_SIBLING_INDEX = 28;
    public static final int CHILD_INDEX = 36;
    public static final int INODE_INDEX = 44;
    public static final int PARENT_INODE_INDEX = 52;
    public static final int FLAGS_INDEX = 60;
    public static final int MAGIC_VALUE_2_INDEX = 61;
    public static final int NAME_INDEX = 65;
    public static final int MAGIC_VALUE_3_INDEX = 321;
}
