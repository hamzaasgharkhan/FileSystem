package DiskUtility;

import Constants.FLAGS;
import Constants.VALUES;
import Utilities.BinaryUtilities;

/**
 * This class provides an interface between the ThumbnailStore files and the rest of the filesystem.
 */
public class ThumbnailStoreGateway {
    public static byte[] getDefaultBytes() {
        return new byte[11];
    }
}
