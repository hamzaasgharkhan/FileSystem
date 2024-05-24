package DiskUtility;

import Constants.FLAGS;
import Constants.VALUES;
import Utilities.BinaryUtilities;

import javax.crypto.SecretKey;
import java.nio.file.Path;

/**
 * This class provides an interface between the ThumbnailStore files and the rest of the filesystem.
 */
public class ThumbnailStoreGateway {
    public ThumbnailStoreGateway(Path path, BitMapUtility bitMapUtility, SecretKey key) {
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

    /**
     * This method takes a thumbnailAddress and removes the thumbnail from the FileSystem
     * @param thumbnailAddress Target Thumbnail Address
     */
    public void removeNode(long thumbnailAddress){
        // IMPLEMENT LATER
        // Delete Thumbnail from thumbnail store
        // Change the bitmap
    }
}
