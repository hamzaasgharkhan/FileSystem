package Constants;

public abstract class FLAGS {
    public static final byte DEFAULT_DIRECTORY_FRAME_FILE = (byte) 0b00000000;
    public static final byte DEFAULT_DIRECTORY_FRAME_DIR = (byte) 0b10000000;
    public static final byte DEFAULT_NODE_DIRECTORY = (byte) 0b10000000;
    public static final byte DEFAULT_NODE_FILE = (byte) 0b00000000;
    public static final byte DEFAULT_INODE = (byte) 0b00000000;
    public static final byte INODE_THUMBNAIL = (byte) 0b10000000;
    public static final byte DEFAULT_DATA_STORE_HEADER =(byte) 0b00000000;
    public static final byte DEFAULT_THUMBNAIL_STORE_HEADER = (byte) 0b00000000;
    public static final byte DEFAULT_SUPER_BLOCK = (byte) 0b00000000;
    public static final byte DEFAULT_DIRECTORY_STORE = (byte) 0b00000000;
    public static final byte DEFAULT_FILE_SYSTEM = (byte) 0b00000000;
}
