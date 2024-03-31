package DiskUtility;

import Constants.FLAGS;
import Constants.VALUES;
import Utilities.BinaryUtilities;

import java.nio.file.Path;

/**
 * This class provides an interface between the ThumbnailStore files and the rest of the filesystem.
 */
public class ThumbnailStoreGateway {
    public ThumbnailStoreGateway(Path path, BitMapUtility bitMapUtility) {
        // FIX CONSTRUCTOR
    }

    public static byte[] getDefaultBytes() {
        return new byte[11];
    }

    public long addNode(Path path) {
        // IMPLEMENT LATER
        // CREATE THUMBNAIL
        // ADD THUMBNAIL TO THUMBNAIL_STORE
        // RETURN THUMBNAIL_STORE_ADDRESS
        return -1;
    }
}
