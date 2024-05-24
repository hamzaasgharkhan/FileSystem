package Constants;

public class VALUES {
    /**
     * A fixed integer that is used to verify the validity of frames/files.
     * 0x41717561 is also the ASCII code for Aqua.
     */
    public final static int MAGIC_VALUE = 0x41717561;
    /**
     * A byte array representation of the MAGIC_VALUE
     */
    public final static byte[] MAGIC_VALUE_BYTES = {0x41, 0x71, 0x75, 0x61};
    /**
     * The default maximum size (in MB) of DataStore Files and ThumbnailStore files.
     */
    public final static int DEFAULT_DATAFILE_MAX_SIZE = 1024;

    /**
     * Name of the SuperBlock file.
     */
    public final static String SUPER_BLOCK_PRIMARY_NAME = "gH9sP3fE2oR7aL8w";
    /**
     * Name of the Second SuperBlock file
     */
    public final static String SUPER_BLOCK_SECONDARY_NAME = "aF6nZ2cP8oL1qR9w";
    /**
     * Name for the SuperBlock file to be used while changing the secondary file to the primary file.
     */
    public final static String SUPER_BLOCK_TRANSITION_NAME = "J4kL1pQ8zW2sR7vX";
    public final static int IV_SIZE = 12;
    public final static int TAG_SIZE = 16;
    public final static int SALT_SIZE = 16;
}
