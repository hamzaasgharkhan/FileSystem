package Constants;

public abstract class DATA_STORE_BLOCK_FRAME {
    public static final int DATA_STORE_FRAME_SIZE = 4096;
    public static final int DATA_STORE_FRAME_BITMAP_SIZE = 453;
    public static final int DATA_STORE_FRAME_DATA_SIZE = 3642;

    public static final int MD5_HASH_INDEX = 0;
    public static final int BYTES_OCCUPIED_INDEX = 16;
    public static final int FLAGS_INDEX = 18;
    public static final int BITMAP_INDEX = 19;

    public static final int FIRST_DATA_BYTE_INDEX = 472;
}
