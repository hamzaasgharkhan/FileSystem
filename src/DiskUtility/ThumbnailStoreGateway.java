package DiskUtility;

import Constants.FLAGS;
import Constants.VALUES;
import FileSystem.InputFile;
import Utilities.BinaryUtilities;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Path;

/**
 * This class provides an interface between the ThumbnailStore files and the rest of the filesystem.
 */
public class ThumbnailStoreGateway extends DataStoreGateway{
    public ThumbnailStoreGateway(File baseFile, BitMapUtility bitMapUtility, SecretKey key) throws Exception{
        super(baseFile, bitMapUtility, key, Store.ThumbnailStore);
    }
}
