package FileSystem;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is the Input format that the FileSystem accepts when adding new files.
 */
public class InputFile {
    public final String name;
    /**
     * The path should be of linux format and should begin with /
     */
    public final String parentPath;
    public final long size;
    public final long thumbnailSize;
    public final long creationTime;
    public final long lastModifiedTime;
    public final InputStream fileInputStream;
    public final InputStream thumbnailInputStream;

    public InputFile(String name, String parentPath, long size, long creationTime, long lastModifiedTime, InputStream fileInputSteam, InputStream thumbnailInputStream, long thumbnailSize){
        this.name = name;
        this.parentPath = parentPath;
        this.size = size;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
        this.fileInputStream = fileInputSteam;
        this.thumbnailInputStream = thumbnailInputStream;
        this.thumbnailSize = thumbnailSize;
    }

    public InputFile(String name, String parentPath, long size, long creationTime, long lastModifiedTime, InputStream fileInputStream){
        this(name, parentPath, size, creationTime, lastModifiedTime, fileInputStream, null, -1);
    }

    public void close() throws IOException {
        if (fileInputStream != null){
            fileInputStream.close();
        }
        if (thumbnailInputStream != null){
            thumbnailInputStream.close();
        }
    }
}
