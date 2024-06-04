package Constants;
/**
 *     Magic Value                 -       4 bytes                                 || Starting Index: 0
 *     DataStore Index             -       8 bytes                                 || Starting Index: 4
 *     DataStore Offset            -       4 bytes                                 || Starting Index: 12
 *     Length                      -       8 bytes                                 || Starting Index: 16
 */
public abstract class EXTENT_STORE_FRAME {
    public static final int SIZE = 32;
    public static final int FULL_SIZE = SIZE + VALUES.IV_SIZE + VALUES.TAG_SIZE;
    public static final int MAGIC_VALUE_INDEX = 0;
    public static final int DATA_STORE_INDEX_INDEX = 4;
    public static final int DATA_STORE_OFFSET_INDEX = 12;
    public static final int LENGTH_INDEX = 16;
    public static final int NEXT_EXTENT_ADDRESS_INDEX = 24;

}
